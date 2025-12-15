import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../../hooks/useAuth';
import { useServerConfig } from '../../../hooks/useServerConfig';

interface ProtectedRouteProps {
  children?: React.ReactNode; 
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated, isVerified, isLoading } = useAuth();

  if (!serverConfig.isSecured) {
    return children ? <>{children}</> : <Outlet />;
  }

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isVerified) {
    const currentPath = window.location.pathname;
    if (currentPath !== '/verify') {
        return <Navigate to="/verify" replace />;
    }
  }

  return children ? <>{children}</> : <Outlet />;
};

export default ProtectedRoute;