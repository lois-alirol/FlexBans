const AUTH_STORAGE_KEY = 'authStorageType';
const AUTH_TOKEN_KEY = 'authToken';
const USER_DATA_KEY = 'userData';

const getActiveStorage = (): Storage | null => {
  const type = localStorage.getItem(AUTH_STORAGE_KEY);
  
  if (type === 'local') {
    return localStorage;
  }
  if (type === 'session') {
    if (sessionStorage.getItem(AUTH_TOKEN_KEY)) {
        return sessionStorage;
    }
  }
  
  if (localStorage.getItem(AUTH_TOKEN_KEY)) {
      return localStorage;
  }

  return null;
};

const getTargetStorage = (stayLoggedIn: boolean): Storage => {
  return stayLoggedIn ? localStorage : sessionStorage;
};

export const setAuthToken = (token: string, stayLoggedIn: boolean) => {
  const targetStorage = getTargetStorage(stayLoggedIn);
  const type = stayLoggedIn ? 'local' : 'session';
  
  localStorage.setItem(AUTH_STORAGE_KEY, type);
  targetStorage.setItem(AUTH_TOKEN_KEY, token);

  if (stayLoggedIn) {
    sessionStorage.removeItem(AUTH_TOKEN_KEY);
  } else {
    localStorage.removeItem(AUTH_TOKEN_KEY);
  }
};

export const getAuthToken = (): string | null => {
  const storage = getActiveStorage();
  return storage ? storage.getItem(AUTH_TOKEN_KEY) : null;
};

export const setUserData = (user: object, stayLoggedIn: boolean) => {
  const targetStorage = getTargetStorage(stayLoggedIn);

  targetStorage.setItem(USER_DATA_KEY, JSON.stringify(user));

  if (stayLoggedIn) {
    sessionStorage.removeItem(USER_DATA_KEY);
  } else {
    localStorage.removeItem(USER_DATA_KEY);
  }
};

export const getUserData = () => {
  const storage = getActiveStorage();
  if (!storage) return null;

  const data = storage.getItem(USER_DATA_KEY);
  if (!data) return null;

  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
};

export const clearAuthData = () => {
  localStorage.removeItem(AUTH_STORAGE_KEY);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(USER_DATA_KEY);
  sessionStorage.removeItem(AUTH_TOKEN_KEY);
  sessionStorage.removeItem(USER_DATA_KEY);
};

export const removeAuthToken = () => {
  clearAuthData();
};