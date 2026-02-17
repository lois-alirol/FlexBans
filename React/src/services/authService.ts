import { clearAuthData, setAuthToken, setUserData } from "../utils/tokenUtils";

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const AUTH_API_URL = `${BASE_API_URL}/auth`;

// CSRF caching and deduplication
let csrfToken: string | null = null;
let csrfExpiresAt = 0; // epoch ms when token should be considered stale
let inflightCsrfPromise: Promise<string> | null = null;
let inflightCsrfRefresh: Promise<string> | null = null;

// TTL: align with server cookie Max-Age (3600s), refresh a bit earlier
const CSRF_TTL_MS = 55 * 60 * 1000; // 55 minutes

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

/**
 * Fetch CSRF token from the server with in-flight deduplication.
 */
export const fetchCsrfToken = async (): Promise<string> => {
  if (inflightCsrfPromise) {
    return inflightCsrfPromise;
  }

  inflightCsrfPromise = (async () => {
    const response = await fetch(`${AUTH_API_URL}/csrf-token`, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    });

    if (!response.ok) {
      throw new Error('Failed to fetch CSRF token');
    }

    const data: ApiResponse<{ csrfToken: string }> = await response.json();
    // Server may return {data: {csrfToken}} or {csrfToken}
    const tokenFromBody = data.data?.csrfToken || (data as any).csrfToken;
    const tokenFromCookie = readCookie('XSRF-TOKEN');

    csrfToken = tokenFromBody || tokenFromCookie;
    if (!csrfToken) {
      throw new Error('CSRF token not found in response');
    }

    // Refresh TTL
    csrfExpiresAt = Date.now() + CSRF_TTL_MS;

    return csrfToken;
  })();

  try {
    const token = await inflightCsrfPromise;
    return token;
  } finally {
    inflightCsrfPromise = null;
  }
};

/**
 * Get CSRF token, using cached value or cookie, and dedup server fetches.
 */
export const getCsrfToken = async (): Promise<string> => {
  // 1) Use cached if valid
  if (isCsrfValid()) {
    return csrfToken!;
  }

  // 2) Try to read from cookie (server sets XSRF-TOKEN)
  const cookieToken = readCookie('XSRF-TOKEN');
  if (cookieToken) {
    csrfToken = cookieToken;
    csrfExpiresAt = Date.now() + CSRF_TTL_MS;
    return csrfToken;
  }

  // 3) Fetch from server (deduped)
  return await fetchCsrfToken();
};

export const clearCsrfToken = () => {
  csrfToken = null;
  csrfExpiresAt = 0;
};

const apiCall = async (endpoint: string, method: string, body?: object) => {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  // Only attach CSRF for state-changing methods
  const needsCsrf = method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS';
  if (needsCsrf) {
    try {
      headers['X-CSRF-Token'] = await getCsrfToken();
    } catch (error) {
      console.error('Failed to get CSRF token:', error);
      throw new Error('Security token unavailable. Please refresh the page.');
    }
  }

  const execFetch = async () => {
    const response = await fetch(`${AUTH_API_URL}${endpoint}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'include',
      cache: 'no-store',
    });
    return response;
  };

  try {
    let response = await execFetch();

    // Handle CSRF failures with a single deduped refresh across concurrent calls
    if (response.status === 403) {
      const errorData = await response.json().catch(() => null);

      if (errorData?.message?.includes('CSRF')) {
        clearCsrfToken();

        if (!inflightCsrfRefresh) {
          inflightCsrfRefresh = (async () => {
            try {
              const newToken = await fetchCsrfToken();
              return newToken;
            } finally {
              inflightCsrfRefresh = null;
            }
          })();
        }

        try {
          headers['X-CSRF-Token'] = await inflightCsrfRefresh;
        } catch {
          throw new Error('Failed to refresh CSRF token');
        }

        // Retry once after CSRF refresh
        response = await execFetch();
        if (!response.ok) {
          const retryErrorData = await response.json().catch(() => null);
          throw new Error(retryErrorData?.message ?? 'Forbidden');
        }
      } else {
        throw new Error(errorData?.message ?? 'Forbidden');
      }
    }

    if (!response.ok) {
      if (response.status === 401) {
        clearAuthData();
        clearCsrfToken();
      }
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message ?? response.statusText);
    }

    if (response.status === 204) {
      return null;
    }

    const apiResponse: ApiResponse = await response.json();

    if (apiResponse.code && apiResponse.code >= 400) {
      throw new Error(apiResponse.message || 'API request failed');
    }

    return apiResponse.data ?? apiResponse;
  } catch (error) {
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Network request failed');
  }
};

export const login = async (
    username: string,
    password: string,
    stayLoggedIn: boolean
): Promise<AuthResponse> => {
  if (!username || !password) {
    throw new Error('Username and password are required');
  }

  if (username.length < 3 || username.length > 32) {
    throw new Error('Username must be between 3 and 32 characters');
  }

  const response = await apiCall('/login', 'POST', { username, password });

  setAuthToken('', stayLoggedIn);

  const userData = {
    id: response.user.id,
    username: response.user.username,
    isVerified: response.user.isVerified ?? false,
    permissions: response.user.permissions,
  };

  setUserData(userData, stayLoggedIn);

  return {
    user: userData,
  };
};

export const logout = async () => {
  try {
    await apiCall('/logout', 'POST');
  } catch (error) {
    console.error('Logout request failed:', error);
  } finally {
    clearAuthData();
    clearCsrfToken();
  }
};

export const register = async (
    username: string,
    password: string,
    stayLoggedIn = true
): Promise<AuthResponse> => {
  if (!username || !password) {
    throw new Error('Username and password are required');
  }

  if (username.length < 3 || username.length > 32) {
    throw new Error('Username must be between 3 and 32 characters');
  }

  if (password.length < 8) {
    throw new Error('Password must be at least 8 characters');
  }

  const response = await apiCall('/register', 'POST', { username, password });

  setAuthToken('', stayLoggedIn);

  const userData = {
    id: response.user.id,
    username: response.user.username,
    isVerified: response.user.isVerified ?? false,
    permissions: response.user.permissions,
  };

  setUserData(userData, stayLoggedIn);

  return {
    user: userData,
  };
};

export const request2FACode = async (): Promise<TwoFAResponse> => {
  const response = await apiCall('/2fa/request', 'POST');

  if (response.already_verified) {
    return { code: '', already_verified: true };
  }

  return {
    code: response.code,
    already_verified: false,
  };
};

export const getMe = async (): Promise<MeResponse> => {
  return await apiCall('/me', 'GET');
};

export const verify2FA = async (code: string): Promise<AuthResponse> => {
  if (!code || code.length !== 6) {
    throw new Error('Invalid verification code');
  }

  const response = await apiCall('/verify-2fa', 'POST', { code });

  const userData = {
    id: response.user.id,
    username: response.user.username,
    isVerified: response.user.isVerified ?? true,
    permissions: response.user.permissions,
  };

  setUserData(userData, true);

  return {
    user: userData,
  };
};

export const refreshAuthToken = async (): Promise<boolean> => {
  try {
    await apiCall('/refresh', 'POST');
    return true;
  } catch {
    clearAuthData();
    return false;
  }
};

export default {
  fetchCsrfToken,
  getCsrfToken,
  clearCsrfToken,
  login,
  logout,
  register,
  request2FACode,
  getMe,
  verify2FA,
  refreshAuthToken,
};