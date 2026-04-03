import React, { useState, useEffect, useMemo, useCallback } from 'react';

import {
    AuthContext,
    type AuthContextType,
    type UserContext
} from '@components/common/auth/AuthContext';

import { getUserData, clearAuthData, setUserData } from '@utils/tokenUtils';
import authService from '@services/authService';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserContext | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUserFromBackend = useCallback(async () => {
    try {
      const meData = await authService.getMe();

      const updatedUser: UserContext = {
        id: meData.username,
        username: meData.username,
        isVerified: meData.is_verified,
        permissions: meData.permissions,
      };

      setUser(updatedUser);
      setUserData(updatedUser, localStorage.getItem('stayLoggedIn') === 'true');

      return updatedUser;
    } catch (err) {
      console.error('Failed to fetch user from backend:', err);
      return null;
    }
  }, []);

  useEffect(() => {
    const initAuth = async () => {
      try {
        await authService.fetchCsrfToken().catch((err: any) => {
          console.warn('Initial CSRF fetch failed:', err);
        });

        const userData = getUserData();

        if (userData) {
          setUser(userData);

          if (userData.isVerified) {
            const backendUser = await fetchUserFromBackend();
            if (backendUser) {
              setUser(backendUser);
            }
          }
        } else {
          clearAuthData();
          setUser(null);
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        clearAuthData();
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();
  }, [fetchUserFromBackend]);

  const refreshUserVerification = useCallback(async () => {
    try {
      const user = await fetchUserFromBackend();
      if (user) {
        setUser(user);
      } else {
        clearAuthData();
        setUser(null);
      }
    } catch (err) {
      console.error('Failed to refresh user:', err);
      clearAuthData();
      setUser(null);
    }
  }, [fetchUserFromBackend]);

  const value: AuthContextType = useMemo(() => ({
    user,
    isAuthenticated: !!user,
    isVerified: user?.isVerified ?? false,
    login: async (username: string, password: string, stayLoggedIn: boolean) => {
      const response = await authService.login(username, password, stayLoggedIn);
      setUser(response.user);
      return response;
    },
    logout: () => {
      authService.logout();
      setUser(null);
    },
    refreshUserVerification,
    isLoading,
  }), [user, isLoading, refreshUserVerification]);

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
);
};