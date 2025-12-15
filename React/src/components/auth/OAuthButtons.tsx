import React from 'react';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useTheme } from '../../hooks/useTheme';

interface OAuthButtonsProps {
  action: 'login' | 'register';
  onOAuthClick: (service: string) => void;
}

export const OAuthButtons: React.FC<OAuthButtonsProps> = ({ action, onOAuthClick }) => {
  const { enabledProviders } = useServerConfig();
  const { currentTheme } = useTheme();
  
  const isDark = currentTheme === 'dark';

  if (enabledProviders.length === 0) return null;

  const dividerColorClass = isDark ? 'border-[#333333]' : 'border-[#e4e4e7]';
  const textClass = isDark ? 'text-[#a1a1aa]' : 'text-[#71717a]';

  return (
    <div className="pt-2 space-y-3">
      <div className="relative flex items-center">
        <div className={`grow border-t ${dividerColorClass}`}></div>
        <span className={`shrink mx-4 text-xs uppercase font-medium ${textClass}`}>
          or {action} with
        </span>
        <div className={`grow border-t ${dividerColorClass}`}></div>
      </div>

      <div className="flex justify-center space-x-4">
        {enabledProviders.map(provider => (
          <button
            key={provider.service}
            title={`${action} with ${provider.service}`}
            type="button"
            className="p-3 rounded-md text-white text-xl transition duration-300 transform hover:scale-110 active:scale-95 shadow-md"
            style={{ backgroundColor: provider.color }}
            onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = provider.hoverColor)}
            onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = provider.color)}
            onClick={() => onOAuthClick(provider.service)}
          >
            {provider.icon}
          </button>
        ))}
      </div>
    </div>
  );
};