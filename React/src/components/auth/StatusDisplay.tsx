import React from 'react';

import type { StatusMessage } from '@/types/auth';

interface StatusDisplayProps {
  status: StatusMessage;
}

export const StatusDisplay: React.FC<StatusDisplayProps> = ({ status }) => {

  const getStatusClasses = (type: StatusMessage['type']) => {
    if (type === 'success') return 'bg-success border-success-border text-text-success';
    if (type === 'error') return 'bg-error border-error-border text-text-error';
    return 'hidden';
  };

  return (
    <div
      className={`text-sm font-medium px-4 py-2 rounded-lg text-center border ${getStatusClasses(status.type)} transition-opacity duration-300`}
    >
      {status.text}
    </div>
  );
};