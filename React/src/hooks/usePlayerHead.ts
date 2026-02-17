import { useMemo } from 'react';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const PLACEHOLDER_HEAD = 'https://mc-heads.net/avatar/steve/32';

export const usePlayerHead = (username: string, size = 32) => {
    const headUrl = useMemo(() => {
        if (!username) return PLACEHOLDER_HEAD;
        return `${BASE_API_URL}/player/head/${encodeURIComponent(username)}`;
    }, [username, size]);

    return { headUrl };
};

export { PLACEHOLDER_HEAD };