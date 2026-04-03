import { useState, useCallback } from 'react';

import authService from '@services/authService';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const REVOKE_API_URL = `${BASE_API_URL}/punishments/revoke`;

interface RevocationPayload {
    punishmentId: number;
    punishmentType: string;
    reason: string;
    silent?: boolean;
}

export const usePunishmentRevocation = () => {
    const [isRevoking, setIsRevoking] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const revokePunishment = useCallback(async (payload: RevocationPayload): Promise<void> => {
        setIsRevoking(true);
        setError(null);

        try {
            const csrfToken = await authService.getCsrfToken();

            const response = await fetch(REVOKE_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': csrfToken,
                },
                credentials: 'include',
                body: JSON.stringify(payload),
            });

            const apiResponse = await response.json();

            // Backend returns 201 for success as per your Java code
            if (!response.ok || (apiResponse.code !== 200 && apiResponse.code !== 201)) {
                throw new Error(apiResponse.message || `Error: ${response.status}`);
            }

            console.log('Punishment revoked successfully:', apiResponse);
        } catch (e) {
            const message = e instanceof Error ? e.message : 'An unknown error occurred.';
            setError(message);
            throw e;
        } finally {
            setIsRevoking(false);
        }
    }, []);

    return { revokePunishment, isRevoking, error };
};