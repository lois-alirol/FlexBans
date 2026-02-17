import { useEffect, useMemo, useRef, useState } from 'react';
import type { PunishmentsData, PunishmentType } from '../types/punishments';

const DEFAULT_PUNISHMENTS_DATA: PunishmentsData = {
    recentPunishments: [],
    userStats: {},
    globalCounts: {
        BAN: 0,
        MUTE: 0,
        WARNING: 0,
        KICK: 0,
    },
};

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const BASE_PUNISHMENT_DATA_API_URL = `${BASE_API_URL}/punishments`;

type ApiPaginatedResponse = {
    code: number;
    message?: string;
    data: {
        items: any[];
        page: number;
        perPage: number;
        totalItems: number;
        totalPages: number;
        countsByType?: {
            BAN?: number;
            MUTE?: number;
            WARNING?: number;
            KICK?: number;
        };
    };
};

const inflightRequests = new Map<string, Promise<ApiPaginatedResponse>>();
const responseCache = new Map<string, ApiPaginatedResponse>();

const computeCountsByType = (items: any[]) => {
    const counts = { BAN: 0, MUTE: 0, WARNING: 0, KICK: 0 };
    for (const it of items) {
        const t = String(it?.type || '').toUpperCase();
        if (t in counts) {
            // @ts-expect-error dynamic key guarded by membership check
            counts[t] += 1;
        }
    }
    return counts;
};

const buildUrl = (baseUrl: string, page: number, type?: PunishmentType | null) => {
    const safePage = Math.max(1, page);
    const url = new URL(baseUrl, window.location.origin);

    url.searchParams.set('page', String(safePage));
    url.searchParams.set('type', type || 'BAN');

    return url.toString();
};

export const usePunishmentsData = (
    apiUrl: string = BASE_PUNISHMENT_DATA_API_URL,
    page: number = 1,
    type?: PunishmentType | null
) => {
    const [punishmentsData, setPunishmentsData] = useState<PunishmentsData>(DEFAULT_PUNISHMENTS_DATA);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [pagination, setPagination] = useState<{
        page: number;
        perPage: number;
        totalItems: number;
        totalPages: number;
    }>({ page: 1, perPage: 20, totalItems: 0, totalPages: 1 });

    const url = useMemo(() => buildUrl(apiUrl, page, type), [apiUrl, page, type]);

    const requestIdRef = useRef(0);

    useEffect(() => {
        const myRequestId = ++requestIdRef.current;
        const cacheKey = `${apiUrl}|page=${page}|type=${type || 'all'}`;

        const run = async () => {
            setIsLoading(true);
            setError(null);

            try {
                const cached = responseCache.get(cacheKey);
                if (cached) {
                    const payload = cached.data;
                    const items = payload.items;

                    const counts =
                        payload.countsByType && typeof payload.countsByType === 'object'
                            ? {
                                BAN: payload.countsByType.BAN ?? 0,
                                MUTE: payload.countsByType.MUTE ?? 0,
                                WARNING: payload.countsByType.WARNING ?? 0,
                                KICK: payload.countsByType.KICK ?? 0,
                            }
                            : computeCountsByType(items);

                    setPunishmentsData({
                        recentPunishments: items,
                        userStats: {},
                        globalCounts: counts,
                    });

                    const derivedTotalPages =
                        payload.totalPages ??
                        (payload.perPage > 0 ? Math.ceil((payload.totalItems ?? items.length) / payload.perPage) : 1);

                    setPagination({
                        page: payload.page ?? page,
                        perPage: payload.perPage ?? items.length,
                        totalItems: payload.totalItems ?? items.length,
                        totalPages: derivedTotalPages,
                    });

                    setIsLoading(false);
                    return;
                }

                let promise = inflightRequests.get(cacheKey);
                if (!promise) {
                    promise = fetch(url, {
                        cache: 'no-store',
                        credentials: 'include',
                    }).then(async (res) => {
                        if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
                        const json = (await res.json()) as ApiPaginatedResponse;
                        if (json.code !== 200) {
                            throw new Error(json.message || 'Failed to fetch punishments data');
                        }
                        responseCache.set(cacheKey, json);
                        return json;
                    }).finally(() => {
                        inflightRequests.delete(cacheKey);
                    });

                    inflightRequests.set(cacheKey, promise);
                }

                const apiResponse = await promise;

                if (myRequestId !== requestIdRef.current) return;

                const payload = apiResponse.data;

                if (!payload || !Array.isArray(payload.items)) {
                    throw new Error('Invalid data structure received from API.');
                }

                const items = payload.items;
                const counts =
                    payload.countsByType && typeof payload.countsByType === 'object'
                        ? {
                            BAN: payload.countsByType.BAN ?? 0,
                            MUTE: payload.countsByType.MUTE ?? 0,
                            WARNING: payload.countsByType.WARNING ?? 0,
                            KICK: payload.countsByType.KICK ?? 0,
                        }
                        : computeCountsByType(items);

                setPunishmentsData({
                    recentPunishments: items,
                    userStats: {},
                    globalCounts: counts,
                });

                const derivedTotalPages =
                    payload.totalPages ??
                    (payload.perPage > 0 ? Math.ceil((payload.totalItems ?? items.length) / payload.perPage) : 1);

                setPagination({
                    page: payload.page ?? page,
                    perPage: payload.perPage ?? items.length,
                    totalItems: payload.totalItems ?? items.length,
                    totalPages: derivedTotalPages,
                });
            } catch (e: any) {
                console.error('Failed to fetch punishments data:', e);
                setError(e instanceof Error ? e.message : 'An unknown error occurred while fetching punishments.');
                setPunishmentsData(DEFAULT_PUNISHMENTS_DATA);
                setPagination({ page: 1, perPage: 20, totalItems: 0, totalPages: 1 });
            } finally {
                if (myRequestId === requestIdRef.current) {
                    setIsLoading(false);
                }
            }
        };

        run();
        console.debug('[usePunishmentsData] requestId:', myRequestId, 'page:', page, 'type:', type, 'url:', url);
    }, [url, apiUrl, page, type]);

    return {
        punishmentsData,
        isLoading,
        error,
        pagination,
    };
};