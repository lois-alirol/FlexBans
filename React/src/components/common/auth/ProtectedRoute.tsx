import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useAuth } from '@hooks/useAuth';
import { useServerConfig } from '@hooks/useServerConfig';

interface ProtectedRouteProps {
  children?: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated, isVerified, isLoading } = useAuth();
  const location = useLocation();

  if (serverConfig && !serverConfig.isSecured) {
    return children ? <>{children}</> : <Outlet />;
  }

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (isVerified) {
    if (location.pathname === '/verify') {
      return <Navigate to="/" replace />;
    }
    return children ? <>{children}</> : <Outlet />;
  }

  if (location.pathname === '/verify') {
    return children ? <>{children}</> : <Outlet />;
  }

  return <Navigate to="/verify" replace />;
};

export default ProtectedRoute;