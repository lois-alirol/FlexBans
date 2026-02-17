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
    punishmentRevocation: false,
    punishmentExecution: false,
    detailsPageEnabled: true,
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

// Simple cached response with TTL to prevent repeat network calls across components/hooks
type CachedEntry<T> = { data: T; ts: number };
const CONFIG_CACHE = new Map<string, CachedEntry<ServerConfig>>();
const INFLIGHT = new Map<string, Promise<ServerConfig>>();
const CONFIG_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

type ApiResponse<T> = { code: number; message?: string; data: T };

export const useServerConfig = (apiUrl: string = BASE_CONFIG_API_URL) => {
    const [config, setConfig] = useState<ServerConfig>(DEFAULT_CONFIG);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        const cacheKey = apiUrl;

        const fetchConfig = async () => {
            setIsLoading(true);
            setError(null);

            // Serve fresh cache if available and not expired
            const cached = CONFIG_CACHE.get(cacheKey);
            const now = Date.now();
            if (cached && now - cached.ts < CONFIG_CACHE_TTL_MS) {
                if (!cancelled) {
                    setConfig(cached.data);
                    setIsLoading(false);
                }
                return;
            }

            // Deduplicate concurrent requests for the same URL
            let promise = INFLIGHT.get(cacheKey);
            if (!promise) {
                promise = fetch(apiUrl, {
                    cache: 'no-store',
                    credentials: 'include', // include cookies if your API uses sessions
                })
                    .then(async (res) => {
                        if (!res.ok) {
                            throw new Error(`HTTP error! status: ${res.status}`);
                        }
                        const apiResponse: ApiResponse<ServerConfig> = await res.json();
                        if (apiResponse.code !== 200 || !apiResponse.data) {
                            throw new Error(apiResponse.message || 'Failed to fetch config');
                        }
                        const data = apiResponse.data;
                        CONFIG_CACHE.set(cacheKey, { data, ts: Date.now() });
                        return data;
                    })
                    .finally(() => {
                        INFLIGHT.delete(cacheKey);
                    });

                INFLIGHT.set(cacheKey, promise);
            }

            try {
                const data = await promise;
                if (!cancelled) {
                    setConfig(data);
                }
            } catch (e: any) {
                console.error('Failed to fetch server configuration:', e);
                if (!cancelled) {
                    setError(e instanceof Error ? e.message : 'An unknown error occurred.');
                    // Keep whatever config we had, avoid thrashing UI
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };

        fetchConfig();
        return () => {
            cancelled = true;
        };
    }, [apiUrl]);

    const enabledProviders = useMemo(() => {
        return allProviders.filter(
            (p) => config.oauth[p.service.toLowerCase() as keyof typeof config.oauth]
        );
    }, [config]);

    return {
        serverConfig: config,
        enabledProviders,
        isLoading,
        error,
    };
};