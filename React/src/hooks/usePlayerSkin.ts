import { useMemo } from 'react';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const PLACEHOLDER_SKIN = 'https://mc-heads.net/skin/steve';

export const usePlayerSkin = (username: string) => {
    const skinUrl = useMemo(() => {
        if (!username) return PLACEHOLDER_SKIN;
        return `${BASE_API_URL}/player/skin/${encodeURIComponent(username)}`;
    }, [username]);

    return { skinUrl };
};

export { PLACEHOLDER_SKIN };