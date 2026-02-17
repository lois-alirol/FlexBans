import { useEffect, useState } from 'react';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const PLAYER_INFO_API_URL = `${BASE_API_URL}/player`;

type PlayerInfo = {
    username: string;
    uuid: string;
    prefix: string;
    permissions: string[];
};

export const usePlayerInfo = (username: string) => {
    const [playerInfo, setPlayerInfo] = useState<PlayerInfo | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!username) {
            setError("Username is required.");
            setLoading(false);
            return;
        }

        const fetchPlayerInfo = async () => {
            setLoading(true);
            setError(null);

            try {
                const response = await fetch(`${PLAYER_INFO_API_URL}/${username}`);
                if (!response.ok) {
                    throw new Error(`Failed to fetch player information. Status: ${response.status}`);
                }

                const { code, data } = await response.json();
                if (code === 200) {
                    setPlayerInfo({
                        ...data,
                        prefix: data.prefix || "",
                    });
                } else {
                    throw new Error("Invalid response code.");
                }
            } catch (err: any) {
                setError(err.message || "An error occurred while fetching player information.");
                setPlayerInfo(null);
            } finally {
                setLoading(false);
            }
        };

        fetchPlayerInfo();
    }, [username]);

    return { playerInfo, loading, error };
};