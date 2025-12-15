import React, { useState, useEffect, useMemo } from 'react';
import { AuthContext } from './AuthContext';
import { getAuthToken, getUserData, clearAuthData } from '../../../utils/tokenUtils';
import authService from '../../../services/authService';

export interface UserContext {
  id: string;
  username: string;
  isVerified: boolean;
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserContext | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = await getAuthToken();
      const userData = await getUserData();

      if (token && userData) {
        setUser(userData);
      } else {
        clearAuthData();
      }

      setIsLoading(false);
    };

    initAuth();
  }, []);

  const value = useMemo(() => ({
    user,
    isAuthenticated: !!user,
    isVerified: user?.isVerified ?? false,
    login: async (...args: Parameters<typeof authService.login>) => {
      const response = await authService.login(...args);
      setUser(response.user);
      return response;
    },
    logout: () => {
      authService.logout();
      setUser(null);
    },
    isLoading,
  }), [user, isLoading]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
