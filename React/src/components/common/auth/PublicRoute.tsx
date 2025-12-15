import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../../hooks/useAuth';
import { useServerConfig } from '../../../hooks/useServerConfig';

interface PublicRouteProps {
  children?: React.ReactNode;
}

const PublicRoute: React.FC<PublicRouteProps> = ({ children }) => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated, isLoading } = useAuth();

  if (!serverConfig.isSecured) {
    return children ? <>{children}</> : <Outlet />;
  }

  if (isLoading) {
    return null;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
};

export default PublicRoute;
