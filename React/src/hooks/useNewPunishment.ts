import { useState, useCallback } from 'react';
import { getAuthToken } from '../utils/tokenUtils';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const NEW_PUNISHMENT_API_URL = `${BASE_API_URL}/punishments/create`;

interface NewPunishmentPayload {
    target: string;
    identity: string;
    punishmentType: string;
    silent: boolean;
    reason: string;
    duration: string | null;
    permanent: boolean;
}

export const useNewPunishment = () => {
    const [isCreating, setIsCreating] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);

    const resetState = useCallback(() => {
        setIsCreating(false);
        setError(null);
        setSuccess(false);
    }, []);

    const createPunishment = useCallback(async (payload: NewPunishmentPayload): Promise<void> => {
        setIsCreating(true);
        setError(null);
        setSuccess(false);

        try {
            const token = getAuthToken();

            if (!token) {
                throw new Error("Authentication token not found. Please log in.");
            }

            const response = await fetch(NEW_PUNISHMENT_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                let errorMessage = `HTTP error! Status: ${response.status}`;
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    console.error('Failed to parse error response:', e);
                }
                throw new Error(errorMessage);
            }
            
            setSuccess(true);

            setTimeout(() => {
                setSuccess(false);
            }, 5000); 

        } catch (e) {
            const message = e instanceof Error ? e.message : 'An unknown error occurred during punishment creation.';
            console.error('Error creating new punishment:', e);
            setError(message);
            throw e;
        } finally {
            setIsCreating(false);
        }
    }, []);

    return { 
        createPunishment, 
        isCreating, 
        error,
        success,
        resetState
    };
};