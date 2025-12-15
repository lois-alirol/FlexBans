import { useState, useCallback } from 'react';
import { getAuthToken } from '../utils/tokenUtils';

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const REVOKE_API_URL = `${BASE_API_URL}/punishments/revoke`; 

interface RevocationPayload {
    punishmentType: string;
    punishmentId: string;
    identity: string;
    removalReason: string;
}

export const usePunishmentRevocation = () => {
    const [isRevoking, setIsRevoking] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const revokePunishment = useCallback(async (payload: RevocationPayload): Promise<void> => {
        setIsRevoking(true);
        setError(null);
        
        try {
            const response = await fetch(REVOKE_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getAuthToken()}`
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                let errorMessage = `HTTP error! status: ${response.status}`;
                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    console.log(e);
                }
                throw new Error(errorMessage);
            }
        } catch (e) {
            const message = e instanceof Error ? e.message : 'An unknown error occurred during revocation.';
            console.error('Error revoking punishment:', e);
            setError(message);
            throw e;
        } finally {
            setIsRevoking(false);
        }
    }, []);

    return { 
        revokePunishment, 
        isRevoking, 
        error 
    };
};