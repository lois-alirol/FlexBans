import { useState, useEffect } from 'react';

import type {PunishmentDetailData} from '@/types/punishments';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const PUNISHMENT_DETAIL_API_URL = `${BASE_API_URL}/punishments`;

export const usePunishmentDetails = (hexId: string | undefined) => {
    const [data, setData] = useState<PunishmentDetailData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!hexId) {
            setData(null);
            setIsLoading(false);
            return;
        }

        const fetchDetails = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const response = await fetch(`${PUNISHMENT_DETAIL_API_URL}/${hexId}`);

                if (response.status === 404) {
                    setData(null);
                    setError('Punishment not found');
                    return;
                }

                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }

                const apiResponse = await response.json();

                if (apiResponse.code !== 200) {
                    throw new Error(apiResponse.message || 'Failed to fetch punishment details');
                }

                const details:  PunishmentDetailData = apiResponse.data;

                if (!details) {
                    setData(null);
                    setError('No data received');
                    return;
                }

                setData(details);

            } catch (e) {
                console.error(`Failed to fetch punishment ${hexId} details:`, e);
                setError(e instanceof Error ? e.message :  'An unknown error occurred.');
                setData(null);
            } finally {
                setIsLoading(false);
            }
        };

        fetchDetails();
    }, [hexId]);

    return {
        punishmentDetails: data,
        isLoading,
        error
    };
};