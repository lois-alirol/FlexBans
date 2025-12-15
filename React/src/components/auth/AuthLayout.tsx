import React, { type ReactNode } from 'react';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useTheme } from '../../hooks/useTheme';

interface AuthLayoutProps {
  title: string;
  children: ReactNode;
  pageTitle: string;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ title, children, pageTitle }) => {
  const { serverConfig } = useServerConfig();
  const { currentTheme } = useTheme();
  
  const isDark = currentTheme === 'dark';

  const containerBgClass = isDark 
    ? 'bg-[#1c1c1c] text-[#e0e0e0]' 
    : 'bg-[#f4f4f5] text-[#18181b]';

  const cardBgClass = isDark 
    ? 'bg-[#242424] border border-[#333333]' 
    : 'bg-white border border-[#e4e4e7] shadow-lg';
  
  const titleColorClass = isDark ? 'text-white' : 'text-gray-900';

  return (
    <div className={`grid place-items-center min-h-screen font-montserrat p-4 sm:p-6 ${containerBgClass}`}>
      <title>{serverConfig.serverName} {pageTitle}</title>
      <div
        className={`p-8 rounded-xl shadow-2xl w-full max-w-sm transition-all duration-500 ${cardBgClass}`}
        style={{'--server-color': serverConfig.serverColor} as React.CSSProperties}
      >
        <h1 className={`text-3xl font-extrabold text-center mb-6 ${titleColorClass}`}>
          {title} <span style={{color: serverConfig.serverColor}}>{serverConfig.serverName}</span>
        </h1>
        {children}
      </div>
    </div>
  );
};