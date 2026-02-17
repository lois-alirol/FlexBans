import { useState, useCallback } from 'react';
import authService from "../services/authService.ts";

const BASE_API_URL = import.meta.env.VITE_APP_API_URL;
const EDIT_API_URL = `${BASE_API_URL}/punishments/edit`;

interface UpdatePayload {
    punishmentId: number;
    reason: string;
    duration: number; // The calculated relative time in milliseconds
}

export const usePunishmentUpdate = () => {
    const [isUpdating, setIsUpdating] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const updatePunishment = useCallback(async (payload: UpdatePayload): Promise<void> => {
        setIsUpdating(true);
        setError(null);

        try {
            const csrfToken = await authService.getCsrfToken();

            // Convert numbers to strings to match Java RequestUtils.getRequiredString
            const body = {
                punishmentId: payload.punishmentId.toString(),
                reason: payload.reason,
                duration: payload.duration.toString(),
            };

            const response = await fetch(EDIT_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-Token': csrfToken,
                },
                credentials: 'include',
                body: JSON.stringify(body),
            });

            const apiResponse = await response.json();

            if (!response.ok || apiResponse.code !== 200) {
                throw new Error(apiResponse.message || `Error: ${response.status}`);
            }

            console.log('Punishment updated successfully:', apiResponse);
        } catch (e) {
            const message = e instanceof Error ? e.message : 'An unknown error occurred.';
            setError(message);
            throw e;
        } finally {
            setIsUpdating(false);
        }
    }, []);

    return { updatePunishment, isUpdating, error };
};