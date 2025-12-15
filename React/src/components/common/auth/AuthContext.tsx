import { createContext } from 'react';
import type authService from '../../../services/authService';
import type { UserContext } from './AuthProvider';

export interface AuthContextType {
  user: UserContext | null;
  isAuthenticated: boolean;
  isVerified: boolean;
  login: typeof authService.login;
  logout: typeof authService.logout;
  isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
