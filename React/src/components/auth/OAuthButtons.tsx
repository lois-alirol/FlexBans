import React from 'react';

import { useServerConfig } from '@hooks/useServerConfig';

interface OAuthButtonsProps {
  action: 'login' | 'register';
  onOAuthClick: (service: string) => void;
}

export const OAuthButtons: React.FC<OAuthButtonsProps> = ({ action, onOAuthClick }) => {
    const { enabledProviders } = useServerConfig();

    if (enabledProviders.length === 0) return null;

    return (
        <div className="pt-4 space-y-4">
            <div className="relative flex items-center">
                <div className={`grow border-t border-surface-border`}></div>
                    <span className="shrink mx-4 text-xs uppercase font-bold tracking-widest text-text-secondary">
                      Or {action} with
                    </span>
                <div className={`grow border-t border-surface-border`}></div>
            </div>

            <div className="flex justify-center gap-4">
                {enabledProviders.map(provider => (
                    <button
                        key={provider.service}
                        type="button"
                        className="p-4 rounded-xl text-text-primary text-xl transition duration-300 transform hover:-translate-y-1 active:scale-95"
                        style={{
                            backgroundColor: provider.color,
                            boxShadow: `0 8px 20px ${provider.color}44`
                        }}
                        onClick={() => onOAuthClick(provider.service)}
                    >
                        {provider.icon}
                    </button>
                ))}
            </div>
        </div>
    );
};