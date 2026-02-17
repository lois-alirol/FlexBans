import { useState, useCallback } from 'react';
import authService from '../services/authService';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const NEW_PUNISHMENT_API_URL = `${BASE_API_URL}/punishments/create`;

interface NewPunishmentPayload {
    target: string;
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
            const csrfToken = await authService.getCsrfToken();

            const response = await fetch(NEW_PUNISHMENT_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': csrfToken,
                },
                credentials: 'include',
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                let errorMessage = `Error: ${response.status}`;
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

            const apiResponse = await response.json();

            if (apiResponse.code && apiResponse.code >= 400) {
                throw new Error(apiResponse.message || 'Failed to create punishment');
            }

            console.log('Punishment created successfully:', apiResponse.data);
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