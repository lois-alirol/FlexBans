import React from 'react';

export interface UserContext {
  id: string;
  username: string;
  isVerified: boolean;
  permissions: string[];
}

export interface AuthContextType {
  user: UserContext | null;
  isAuthenticated: boolean;
  isVerified: boolean;
  isLoading: boolean;
  login: (username: string, password: string, stayLoggedIn:  boolean) => Promise<any>;
  logout: () => void;
  refreshUserVerification: () => Promise<void>;
}

export const AuthContext = React.createContext<AuthContextType | undefined>(undefined);