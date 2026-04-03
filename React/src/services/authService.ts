import { clearAuthData, setAuthToken, setUserData } from '@utils/tokenUtils';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const AUTH_API_URL = `${BASE_API_URL}/auth`;

let csrfToken: string | null = null;
let csrfExpiresAt = 0;
let inflightCsrfPromise: Promise<string> | null = null;
let inflightCsrfRefresh: Promise<string> | null = null;
let inflightTokenRefresh: Promise<boolean> | null = null;
let refreshTimeoutId: number | null = null;

const CSRF_TTL_MS = 55 * 60 * 1000;
const ACCESS_TOKEN_EXPIRY_MS = 900 * 1000;

interface AuthResponse {
  token?: string;
  user: {
    id: string;
    username: string;
    isVerified: boolean;
    permissions: string[];
  };
}

interface MeResponse {
  username: string;
  is_verified: boolean;
  permissions: string[];
}

interface TwoFAResponse {
  code: string;
  username: string;
  already_verified?: boolean;
}

type ApiResponse<T = any> = { code: number; message?: string; data?: T };

const readCookie = (name: string): string | null => {
  if (typeof document === 'undefined') return null;
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop()!.split(';').shift() || null;
  return null;
};

const isCsrfValid = () => csrfToken && Date.now() < csrfExpiresAt;

export const fetchCsrfToken = async (): Promise<string> => {
  if (inflightCsrfPromise) return inflightCsrfPromise;

  inflightCsrfPromise = (async () => {
    const response = await fetch(`${AUTH_API_URL}/csrf-token`, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    });

    if (!response.ok) throw new Error('Failed to fetch CSRF token');

    const data: ApiResponse<{ csrfToken: string }> = await response.json();
    const tokenFromBody = data.data?.csrfToken || (data as any).csrfToken;
    const tokenFromCookie = readCookie('XSRF-TOKEN');

    csrfToken = tokenFromBody || tokenFromCookie;
    if (!csrfToken) throw new Error('CSRF token not found in response');

    csrfExpiresAt = Date.now() + CSRF_TTL_MS;
    return csrfToken;
  })();

  try {
    return await inflightCsrfPromise;
  } finally {
    inflightCsrfPromise = null;
  }
};

export const getCsrfToken = async (): Promise<string> => {
  if (isCsrfValid()) return csrfToken!;
  const cookieToken = readCookie('XSRF-TOKEN');
  if (cookieToken) {
    csrfToken = cookieToken;
    csrfExpiresAt = Date.now() + CSRF_TTL_MS;
    return csrfToken;
  }
  return await fetchCsrfToken();
};

export const clearCsrfToken = () => {
  csrfToken = null;
  csrfExpiresAt = 0;
};

export const scheduleTokenRefresh = () => {
  if (refreshTimeoutId) {
    window.clearTimeout(refreshTimeoutId);
    refreshTimeoutId = null;
  }

  const delay = Math.floor(ACCESS_TOKEN_EXPIRY_MS * 0.8);
  refreshTimeoutId = window.setTimeout(async () => {
    const ok = await refreshAuthToken();
    if (ok) scheduleTokenRefresh();
  }, delay);
};

const apiCall = async (endpoint: string, method: string, body?: object) => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };

  const needsCsrf = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS';
  if (needsCsrf) {
    try {
      headers['X-CSRF-Token'] = await getCsrfToken();
    } catch (error) {
      console.error('Failed to get CSRF token:', error);
      throw new Error('Security token unavailable. Please refresh the page.');
    }
  }

  const execFetch = async () =>
      fetch(`${AUTH_API_URL}${endpoint}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
        credentials: 'include',
        cache: 'no-store',
      });

  let response = await execFetch();

  if (response.status === 403) {
    const errorData = await response.json().catch(() => null);
    if (errorData?.message?.includes('CSRF')) {
      clearCsrfToken();

      if (!inflightCsrfRefresh) {
        inflightCsrfRefresh = (async () => {
          try {
            return await fetchCsrfToken();
          } finally {
            inflightCsrfRefresh = null;
          }
        })();
      }

      headers['X-CSRF-Token'] = await inflightCsrfRefresh;
      response = await execFetch();
    }

    if (!response.ok) {
      const retryErrorData = await response.json().catch(() => null);
      throw new Error(retryErrorData?.message ?? 'Forbidden');
    }
  }

  if (!response.ok) {
    if (response.status === 401) {
      // IMPORTANT: never recursive-refresh the refresh endpoint itself
      if (endpoint === '/refresh') {
        clearAuthData();
        clearCsrfToken();
        throw new Error('Unauthorized');
      }

      if (!inflightTokenRefresh) {
        inflightTokenRefresh = refreshAuthToken().finally(() => {
          inflightTokenRefresh = null;
        });
      }

      const refreshed = await inflightTokenRefresh;

      if (refreshed) {
        response = await execFetch();
        if (response.ok) {
          if (response.status === 204) return null;
          const retryData: ApiResponse = await response.json();
          return retryData.data ?? retryData;
        }
      }

      clearAuthData();
      clearCsrfToken();
    }

    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message ?? response.statusText);
  }

  if (response.status === 204) return null;

  const apiResponse: ApiResponse = await response.json();
  if (apiResponse.code && apiResponse.code >= 400) {
    throw new Error(apiResponse.message || 'API request failed');
  }

  return apiResponse.data ?? apiResponse;
};

export const login = async (username: string, password: string, stayLoggedIn: boolean): Promise<AuthResponse> => {
  if (!username || !password) throw new Error('Username and password are required');
  if (username.length < 3 || username.length > 32) throw new Error('Username must be between 3 and 32 characters');

  const response = await apiCall('/login', 'POST', { username, password });

  setAuthToken('', stayLoggedIn);

  const userData = {
    id: response.user.id,
    username: response.user.username,
    isVerified: response.user.isVerified ?? false,
    permissions: response.user.permissions,
  };

  setUserData(userData, stayLoggedIn);
  scheduleTokenRefresh();
  return { user: userData };
};

export const logout = async () => {
  try {
    await apiCall('/logout', 'POST');
  } catch (error) {
    console.error('Logout request failed:', error);
  } finally {
    clearAuthData();
    clearCsrfToken();
    if (refreshTimeoutId) {
      window.clearTimeout(refreshTimeoutId);
      refreshTimeoutId = null;
    }
  }
};

export const register = async (username: string, password: string, stayLoggedIn = true): Promise<AuthResponse> => {
  if (!username || !password) throw new Error('Username and password are required');
  if (username.length < 3 || username.length > 32) throw new Error('Username must be between 3 and 32 characters');
  if (password.length < 8) throw new Error('Password must be at least 8 characters');

  const response = await apiCall('/register', 'POST', { username, password });

  setAuthToken('', stayLoggedIn);

  const userData = {
    id: response.user.id,
    username: response.user.username,
    isVerified: response.user.isVerified ?? false,
    permissions: response.user.permissions,
  };

  setUserData(userData, stayLoggedIn);
  scheduleTokenRefresh();
  return { user: userData };
};

export const request2FACode = async (): Promise<TwoFAResponse> => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };

  try {
    headers['X-CSRF-Token'] = await getCsrfToken();
  } catch {
    // Continue without CSRF — server will reject if truly required
  }

  const response = await fetch(`${AUTH_API_URL}/2fa/request`, {
    method: 'POST',
    headers,
    credentials: 'include',
    cache: 'no-store',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    const message = errorData?.message ?? '2FA request failed';

    if (response.status === 429) {
      throw new Error(message || 'Too many verification attempts. Please wait before trying again.');
    }

    throw new Error(message);
  }

  const apiResponse: ApiResponse = await response.json();
  const data = apiResponse.data ?? apiResponse;

  if (data.already_verified) {
    return { code: '', username: '', already_verified: true };
  }

  return {
    code: data.code,
    username: data.username,
    already_verified: false,
  };
};

export const completeVerification = async (): Promise<boolean> => {
  try {
    await apiCall('/verify/complete', 'POST');
    scheduleTokenRefresh();
    return true;
  } catch (error) {
    console.error('Failed to complete verification handshake:', error);
    return false;
  }
};

export const getMe = async (): Promise<MeResponse> => {
  return await apiCall('/me', 'GET');
};

export const refreshAuthToken = async (): Promise<boolean> => {
  try {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };

    try {
      headers['X-CSRF-Token'] = await getCsrfToken();
    } catch {
      // Continue without CSRF
    }

    const response = await fetch(`${AUTH_API_URL}/refresh`, {
      method: 'POST',
      headers,
      credentials: 'include',
      cache: 'no-store',
    });

    if (!response.ok) {
      clearAuthData();
      clearCsrfToken();
      return false;
    }

    return true;
  } catch {
    clearAuthData();
    clearCsrfToken();
    return false;
  }
};

export default {
  fetchCsrfToken,
  getCsrfToken,
  clearCsrfToken,
  scheduleTokenRefresh,
  login,
  logout,
  register,
  request2FACode,
  completeVerification,
  getMe,
  refreshAuthToken,
};