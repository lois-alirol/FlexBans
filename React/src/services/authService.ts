import { clearAuthData, setAuthToken, setUserData, getAuthToken } from "../utils/tokenUtils";

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const AUTH_API_URL = `${BASE_API_URL}/auth`;

interface AuthResponse {
  token: string;
  user: {
    id: string;
    username: string;
    isVerified: boolean;
  };
}

const apiCall = async (endpoint: string, method: string, body?: object) => {
  const token = getAuthToken();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${AUTH_API_URL}${endpoint}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message ?? response.statusText);
  }

  return response.status === 204 ? null : response.json();
};

export const login = async (
  username: string,
  password: string,
  stayLoggedIn: boolean
): Promise<AuthResponse> => {
  const response = await apiCall('/login', 'POST', { username, password });

  setAuthToken(response.token, stayLoggedIn);
  setUserData(response.user, stayLoggedIn);

  return response;
};


export const logout = () => {
    clearAuthData();
};

export const register = async (
  username: string,
  password: string,
  stayLoggedIn = true
): Promise<AuthResponse> => {
  const response = await apiCall('/register', 'POST', { username, password });

  setAuthToken(response.token, stayLoggedIn);
  setUserData(response.user, stayLoggedIn);

  return response;
};

export const request2FACode = async (): Promise<{ code: string }> => {
  return apiCall('/2fa/request', 'POST');
};

export const poll2FAStatus = async (verificationCode: string): Promise<{ verified: boolean }> => {
  return apiCall(`/verify-2fa/${verificationCode}/status`, 'GET');
};

export const submit2FACode = async (code: string, token: string): Promise<AuthResponse> => {
    return apiCall('/verify-totp', 'POST', { code, token });
};

export default { login, logout, register, request2FACode, poll2FAStatus, submit2FACode };