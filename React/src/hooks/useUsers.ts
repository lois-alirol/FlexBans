import { useEffect, useMemo, useState } from "react";
import authService from "../services/authService.ts";

type ApiResponse<T> = { code: number; message?: string; data: T };

export type UserRow = {
    id: number;
    username: string;
    minecraftUuid?: string | null;
    verified: boolean;
    verifiedAt?: number;
    discordId?: string | null;
    prefix?: string;
    permissions: string[];
};

type CachedEntry<T> = { data: T; ts: number };

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const USERS_API_URL = `${BASE_API_URL}/users`;

const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
const CACHE = new Map<string, CachedEntry<UserRow[]>>();
const INFLIGHT = new Map<string, Promise<UserRow[]>>();

export const useUsers = (apiUrl: string = USERS_API_URL) => {
    const [users, setUsers] = useState<UserRow[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        const cacheKey = apiUrl;

        const fetchUsers = async () => {
            setIsLoading(true);
            setError(null);

            const cached = CACHE.get(cacheKey);
            const now = Date.now();
            if (cached && now - cached.ts < CACHE_TTL_MS) {
                if (!cancelled) {
                    setUsers(cached.data);
                    setIsLoading(false);
                }
                return;
            }

            let promise = INFLIGHT.get(cacheKey);
            if (!promise) {
                promise = fetch(apiUrl, {
                    cache: "no-store",
                    credentials: "include",
                })
                    .then(async (res) => {
                        if (!res.ok) {
                            throw new Error(`HTTP error! status: ${res.status}`);
                        }

                        const apiResponse: ApiResponse<UserRow[]> = await res.json();
                        if (apiResponse.code !== 200 || !apiResponse.data) {
                            throw new Error(apiResponse.message || "Failed to fetch users");
                        }

                        const data = apiResponse.data;
                        CACHE.set(cacheKey, { data, ts: Date.now() });
                        return data;
                    })
                    .finally(() => {
                        INFLIGHT.delete(cacheKey);
                    });

                INFLIGHT.set(cacheKey, promise);
            }

            try {
                const data = await promise;
                if (!cancelled) setUsers(data);
            } catch (e: any) {
                console.error("Failed to fetch users:", e);
                if (!cancelled) {
                    setError(e instanceof Error ? e.message : "Unknown error");
                }
            } finally {
                if (!cancelled) setIsLoading(false);
            }
        };

        fetchUsers();
        return () => {
            cancelled = true;
        };
    }, [apiUrl]);

    const updateUserPermissions = async (
        username: string,
        add: string[] = [],
        remove: string[] = []
    ): Promise<string[] | { blockedAdd?: string[]; blockedRemove?: string[] } | null> => {
        const url = `${BASE_API_URL}/user/${encodeURIComponent(username)}/permissions`;

        const csrfToken = await authService.getCsrfToken();

        const res = await fetch(url, {
            method: "PATCH",
            credentials: "include",
            cache: "no-store",
            headers: {
                "Content-Type": "application/json",
                'X-CSRF-Token': csrfToken,
            },
            body: JSON.stringify({ add, remove }),
        });

        if (!res.ok) {
            let message = `Failed to update permissions: ${res.status}`;
            let serverError = undefined;
            try {
                const errJson = await res.json();
                serverError = errJson;
                if (errJson.message) message = errJson.message;
            } catch (e) {
                const errText = await res.text();
                message += ` - ${errText}`;
            }
            throw { message, serverError, status: res.status };
        }

        const apiResponse: ApiResponse<string[]> | ApiResponse<{ blockedAdd?: string[]; blockedRemove?: string[] }> = await res.json();
        if (apiResponse.code !== 200 || !apiResponse.data) {
            throw new Error(apiResponse.message || "Failed to update permissions");
        }

        // Update cache
        const cached = CACHE.get(apiUrl);
        if (cached) {
            const updatedUsers = cached.data.map((u) =>
                u.username === username ? { ...u, permissions: Array.isArray(apiResponse.data) ? apiResponse.data : u.permissions } : u
            );
            CACHE.set(apiUrl, { data: updatedUsers, ts: Date.now() });

            setUsers(updatedUsers);
        }

        return apiResponse.data;
    };

    const userCount = useMemo(() => users.length, [users]);
    const activeCount = 0;

    return {
        users,
        userCount,
        activeCount,
        isLoading,
        error,
        updateUserPermissions,
    };
};