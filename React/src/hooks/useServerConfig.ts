import { useState, useEffect, useMemo } from 'react';

import { allProviders } from '@/types/auth';
import type { ServerConfig } from '@/types/config';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const BASE_CONFIG_API_URL = `${BASE_API_URL}/config`;

type CachedEntry<T> = { data: T; ts: number };
const CONFIG_CACHE = new Map<string, CachedEntry<ServerConfig>>();
const INFLIGHT = new Map<string, Promise<ServerConfig>>();
const CONFIG_CACHE_TTL_MS = 5 * 60 * 1000;

type ApiResponse<T> = { code: number; message?: string; data: T };

export const useServerConfig = (apiUrl: string = BASE_CONFIG_API_URL) => {
    const [config, setConfig] = useState<ServerConfig | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        const cacheKey = apiUrl;

        const fetchConfig = async () => {
            const cached = CONFIG_CACHE.get(cacheKey);
            const now = Date.now();

            if (cached && now - cached.ts < CONFIG_CACHE_TTL_MS) {
                if (!cancelled) {
                    setConfig(cached.data);
                    setIsLoading(false);
                    setError(null);
                }
                return;
            }

            setIsLoading(true);
            setError(null);

            let promise = INFLIGHT.get(cacheKey);
            if (!promise) {
                promise = fetch(apiUrl, {
                    cache: 'no-store',
                    credentials: 'include',
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
                    setIsLoading(false);
                }
            } catch (e: any) {
                if (!cancelled) {
                    setError(e instanceof Error ? e.message : 'Unknown error');
                    setIsLoading(false);
                }
            }
        };

        fetchConfig();
        return () => {
            cancelled = true;
        };
    }, [apiUrl]);

    const enabledProviders = useMemo(() => {
        if (!config) return [];

        return allProviders.filter(
            (p) => config.oauth[p.service.toLowerCase() as keyof typeof config.oauth]
        );
    }, [config]);

    useEffect(() => {
        if (!config) return;

        const root = document.documentElement;

        root.style.setProperty("--color-selection-bg", `${config.serverColor}EE`);

        const getReadableTextColor = (hex: string) => {
            if (!hex?.startsWith("#") || hex.length < 7) return "#ffffff";

            const r = parseInt(hex.slice(1, 3), 16);
            const g = parseInt(hex.slice(3, 5), 16);
            const b = parseInt(hex.slice(5, 7), 16);

            const brightness = (r * 299 + g * 587 + b * 114) / 1000;
            return brightness > 128 ? "#000000" : "#ffffff";
        };

        root.style.setProperty(
            "--color-selection-text",
            getReadableTextColor(config.serverColor)
        );

        root.style.setProperty('--color-server-color', config.serverColor);
        root.style.setProperty('--color-server-color-hover', config.serverColorHover);
    }, [config]);

    return {
        serverConfig: config,
        enabledProviders,
        isLoading,
        error,
    };
};