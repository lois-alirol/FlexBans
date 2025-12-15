import { useState, useEffect } from 'react';
import type { PunishmentsData } from '../types/punishments';

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

export const usePunishmentsData = (apiUrl: string = BASE_PUNISHMENT_DATA_API_URL) => {
    const [punishmentsData, setPunishmentsData] = useState<PunishmentsData>(DEFAULT_PUNISHMENTS_DATA);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchPunishments = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const response = await fetch(apiUrl);

                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }

                const data: PunishmentsData = await response.json();
                
                if (!data || !data.recentPunishments || !data.userStats || !data.globalCounts) {
                     throw new Error('Invalid data structure received from API.');
                }
                
                setPunishmentsData(data);
            } catch (e) {
                console.error('Failed to fetch punishments data:', e);
                setError(e instanceof Error ? e.message : 'An unknown error occurred while fetching punishments.');
            } finally {
                setIsLoading(false);
            }
        };

        fetchPunishments();
    }, [apiUrl]);

    return { 
        punishmentsData, 
        isLoading, 
        error 
    };
};