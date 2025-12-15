import React from 'react';
import type { StatusMessage } from '../../types/auth';
import { useTheme } from '../../hooks/useTheme';

interface StatusDisplayProps {
  status: StatusMessage;
}

export const StatusDisplay: React.FC<StatusDisplayProps> = ({ status }) => {
  const { currentTheme } = useTheme();
  
  const isDark = currentTheme === 'dark';
  
  const getStatusClasses = (type: StatusMessage['type']) => {
    if (type === 'success') return isDark ? 'bg-green-600' : 'bg-green-500';
    if (type === 'error') return isDark ? 'bg-red-600' : 'bg-red-500';
    return 'hidden';
  };

  return (
    <div
      className={`text-sm font-medium px-4 py-2 rounded-lg text-white text-center ${getStatusClasses(status.type)} transition-opacity duration-300`}
    >
      {status.text}
    </div>
  );
};