import { useState, useEffect } from 'react';

export interface PunishmentDetailData {
  punishment_id: string;
  punishment_type: string;
  player_name: string;
  executor: string;
  reason: string;
  execution_date: string;
  expiration_date: string;
  duration: string;
  origin_server: string;
  scope_server: string;
  remover_name: string;
  removal_reason?: string;
  status: 'Active' | 'Removed' | 'Expired';
}

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const PUNISHMENT_DETAIL_API_URL = `${BASE_API_URL}/punishments`;

export const usePunishmentDetails = (id: string | undefined) => {
    const [data, setData] = useState<PunishmentDetailData | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!id) {
            setData(null);
            setIsLoading(false);
            return;
        }

        const fetchDetails = async () => {
            setIsLoading(true);
            setError(null);
            try {
                const response = await fetch(`${PUNISHMENT_DETAIL_API_URL}/${id}`);

                if (response.status === 404) {
                    setData(null);
                    return;
                }
                
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }

                const details: PunishmentDetailData = await response.json();
                setData(details);

            } catch (e) {
                console.error(`Failed to fetch punishment ${id} details:`, e);
                setError(e instanceof Error ? e.message : 'An unknown error occurred.');
                setData(null);
            } finally {
                setIsLoading(false);
            }
        };

        fetchDetails();
    }, [id]);

    return { 
        punishmentDetails: data, 
        isLoading, 
        error 
    };
};