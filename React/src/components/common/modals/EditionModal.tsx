import React, { useEffect, useState } from "react";
import { FaTimes } from "react-icons/fa";
import { useServerConfig } from "../../../hooks/useServerConfig.ts";
import { usePunishmentUpdate } from "../../../hooks/usePunishmentUpdate.ts";

type Theme = "dark" | "light";

interface EditionModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess?: () => void;
    currentTheme: Theme;
    punishmentId: number;
    punishmentType: string;
    initialExpiry?: string; // 'YYYY-MM-DD' or 'Never'
}

const EditionModal: React.FC<EditionModalProps> = ({
                                                       isOpen,
                                                       onClose,
                                                       onSuccess,
                                                       currentTheme,
                                                       punishmentId,
                                                       initialExpiry,
                                                   }) => {
    const [newReason, setNewReason] = useState("");
    const [newExpiry, setNewExpiry] = useState("");
    const [isPermanent, setIsPermanent] = useState(false);

    const { serverConfig } = useServerConfig();
    const { updatePunishment, isUpdating, error } = usePunishmentUpdate();

    // Helper to format the string for the HTML input (YYYY-MM-DDThh:mm)
    const formatDateForInput = (expiryStr?: string) => {
        if (!expiryStr || expiryStr === "Never") {
            // Default to 1 hour from now if "Never" or undefined is passed
            const date = new Date(Date.now() + 3600000);
            return date.toISOString().slice(0, 16);
        }

        // If it's YYYY-MM-DD, we append a default time so the input accepts it
        const date = new Date(expiryStr);
        // Check if date is valid
        if (isNaN(date.getTime())) {
            return new Date(Date.now() + 3600000).toISOString().slice(0, 16);
        }
        return date.toISOString().slice(0, 16);
    };

    useEffect(() => {
        if (isOpen) {
            setNewReason("");
            setIsPermanent(initialExpiry === "Never");
            setNewExpiry(formatDateForInput(initialExpiry));
            document.body.classList.add("overflow-hidden");
        } else {
            document.body.classList.remove("overflow-hidden");
        }
        return () => document.body.classList.remove("overflow-hidden");
    }, [isOpen, initialExpiry]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newReason.trim() || isUpdating) return;

        try {
            // If permanent, duration is usually -1 or 0 depending on your backend API
            let calculatedDuration = -1;

            if (!isPermanent) {
                const selectedTimestamp = new Date(newExpiry).getTime();
                const now = Date.now();
                calculatedDuration = selectedTimestamp - now;
            }

            await updatePunishment({
                punishmentId,
                reason: newReason.trim(),
                duration: calculatedDuration
            });

            if (onSuccess) onSuccess();
            onClose();
        } catch (err) {
            console.error("Update failed:", err);
        }
    };

    if (!isOpen) return null;

    const isDark = currentTheme === "dark";
    const modalClasses = isDark
        ? "bg-[#151515e6] text-white border border-white/10"
        : "bg-white/90 text-gray-900 border border-black/5";

    const inputClasses = isDark
        ? "bg-white/5 border border-white/10 text-white placeholder-white/60 focus:ring-2 disabled:opacity-50"
        : "bg-gray-50 border border-gray-200 text-gray-900 placeholder-gray-500 focus:ring-2 disabled:opacity-50";

    const accentColor = serverConfig.serverColor || "#3b82f6";

    return (
        <div
            className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-black/40 flex items-center justify-center z-[200] p-4"
            onClick={onClose}
        >
            <div
                className={`relative w-full max-w-md max-h-[90vh] rounded-2xl shadow-[0_18px_55px_rgba(0,0,0,0.45)] overflow-hidden flex flex-col ${modalClasses}`}
                onClick={(e) => e.stopPropagation()}
            >
                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-white/6 text-gray-400 hover:text-blue-400 hover:bg-blue-400/10 transition duration-200"
                    disabled={isUpdating}
                >
                    <FaTimes />
                </button>

                <div className="p-6 pb-3 flex items-center justify-between gap-4 pr-14">
                    <h2 className="text-2xl font-bold">Edit Punishment</h2>
                    <span className="px-3 py-1 text-xs font-semibold rounded-full bg-blue-500/15 border border-blue-500/30 text-blue-300">
                        ID: #{punishmentId}
                    </span>
                </div>

                <div className="px-6 pb-6 flex-1 overflow-y-auto text-left">
                    <form onSubmit={handleSubmit} className="space-y-5">
                        {error && (
                            <div className="p-3 text-sm bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg">
                                {error}
                            </div>
                        )}

                        <div className="space-y-3">
                            <div className="flex items-center justify-between">
                                <label className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                    New Expiry Date
                                </label>
                                <label className="flex items-center gap-2 cursor-pointer text-sm">
                                    <input
                                        type="checkbox"
                                        checked={isPermanent}
                                        onChange={(e) => setIsPermanent(e.target.checked)}
                                        className="rounded border-gray-400"
                                    />
                                    Permanent
                                </label>
                            </div>

                            {!isPermanent && (
                                <input
                                    type="datetime-local"
                                    className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
                                    style={{ '--tw-ring-color': `${accentColor}66` } as any}
                                    value={newExpiry}
                                    onChange={(e) => setNewExpiry(e.target.value)}
                                    disabled={isUpdating}
                                    required={!isPermanent}
                                />
                            )}
                            {isPermanent && (
                                <div className={`w-full rounded-lg px-3 py-2 text-base italic opacity-70 ${inputClasses}`}>
                                    This punishment will not expire.
                                </div>
                            )}
                        </div>

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                Reason for Change
                            </label>
                            <textarea
                                className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
                                style={{ '--tw-ring-color': `${accentColor}66` } as any}
                                rows={4}
                                value={newReason}
                                onChange={(e) => setNewReason(e.target.value)}
                                required
                                disabled={isUpdating}
                                placeholder="Why are you modifying this punishment?"
                            />
                        </div>

                        <div className="flex justify-end gap-3 pt-2">
                            <button
                                type="button"
                                onClick={onClose}
                                className="px-4 py-2 rounded-xl bg-white/10 text-gray-200 hover:bg-white/15 transition duration-200"
                                disabled={isUpdating}
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                className="px-6 py-2 rounded-xl text-white font-semibold transition duration-200 disabled:opacity-60"
                                style={{
                                    backgroundColor: accentColor,
                                    boxShadow: `0 10px 25px ${accentColor}55`,
                                }}
                                disabled={isUpdating || !newReason.trim()}
                            >
                                {isUpdating ? "Saving..." : "Save Changes"}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default EditionModal;