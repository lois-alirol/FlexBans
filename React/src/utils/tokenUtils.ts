const USER_DATA_KEY = 'userData';

const getActiveStorage = (): Storage | null => {
  return localStorage;
};

export const setAuthToken = (_token: string, stayLoggedIn: boolean) => {
  localStorage.setItem('stayLoggedIn', stayLoggedIn.toString());
};

export const getAuthToken = (): string | null => {
  return null;
};

export const setUserData = (user: object, _stayLoggedIn: boolean) => {
  const safeUserData = {
    username: (user as any).username,
    id: (user as any).id,
    isVerified: (user as any).isVerified,
    lastUpdated: new Date().toISOString(),
  };
  localStorage.setItem(USER_DATA_KEY, JSON.stringify(safeUserData));
};

export const getUserData = () => {
  const storage = getActiveStorage();
  if (!storage) return null;

  const data = storage.getItem(USER_DATA_KEY);
  if (!data) return null;

  try {
    const parsed = JSON.parse(data);
    if (!parsed.username || !parsed.id) return null;
    return parsed;
  } catch {
    return null;
  }
};

export const clearAuthData = () => {
  localStorage.removeItem('stayLoggedIn');
  localStorage.removeItem(USER_DATA_KEY);
};

export const removeAuthToken = () => {
  clearAuthData();
};