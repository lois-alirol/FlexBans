import { useState, useEffect, useMemo } from 'react';
import { allProviders } from '../types/auth'; 
import type { ServerConfig } from '../types/config';

const DEFAULT_CONFIG: ServerConfig = {
    serverName: 'Loading...',
    serverDescription: 'Fetching configuration data...',
    serverFavicon: '/favicon.ico',
    serverLogo: '/logo.png',
    serverColor: '#6b7280',
    serverColorHover: '#4b5563',
    isSecured: false,
    punishments: {
        bans: { enabled: false, maxPerPage: 20 },
        mutes: { enabled: false, maxPerPage: 20 },
        warnings: { enabled: false, maxPerPage: 20 },
        kicks: { enabled: false, maxPerPage: 20 },
    },
    histories: {
        playerMaxPerPage: 20,
        moderatorMaxPerPage: 20,
    },
    oauth: { discord: false, google: false, github: false, x: false },
};

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const BASE_CONFIG_API_URL = `${BASE_API_URL}/config`;

export const useServerConfig = (apiUrl: string = BASE_CONFIG_API_URL) => {
    const [config, setConfig] = useState<ServerConfig>(DEFAULT_CONFIG);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchConfig = async () => {
            try {
                const response = await fetch(apiUrl);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const data: ServerConfig = await response.json();
                setConfig(data);
            } catch (e) {
                console.error('Failed to fetch server configuration:', e);
                setError(e instanceof Error ? e.message : 'An unknown error occurred.');
            } finally {
                setIsLoading(false);
            }
        };

        fetchConfig();
    }, [apiUrl]);

    const enabledProviders = useMemo(() => {
        return allProviders.filter(
            p => config.oauth[p.service.toLowerCase() as keyof typeof config.oauth]
        );
    }, [config]);

    return { 
        serverConfig: config, 
        enabledProviders, 
        isLoading, 
        error 
    };
};