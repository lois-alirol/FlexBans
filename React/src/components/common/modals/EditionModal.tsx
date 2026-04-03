import React, { useEffect, useState } from 'react';
import { FaTimes } from 'react-icons/fa';

import { usePunishmentUpdate } from '@hooks/usePunishmentUpdate';
import {useTranslation} from "react-i18next";

interface EditionModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess?: () => void;
    punishmentId: number;
    punishmentType: string;
    initialExpiry?: string;
}

const EditionModal: React.FC<EditionModalProps> = ({
                                                       isOpen,
                                                       onClose,
                                                       onSuccess,
                                                       punishmentId,
                                                       initialExpiry,
                                                   }) => {
    const [newReason, setNewReason] = useState("");
    const [newExpiry, setNewExpiry] = useState("");
    const [isPermanent, setIsPermanent] = useState(false);

    const { updatePunishment, isUpdating, error } = usePunishmentUpdate();
    const { t } = useTranslation();

    const formatDateForInput = (expiryStr?: string) => {
        if (!expiryStr || expiryStr === "Never") {
            const date = new Date(Date.now() + 3600000);
            return date.toISOString().slice(0, 16);
        }

        const date = new Date(expiryStr);
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

    const modalClasses = "bg-modal-background text-modal-text-primary border border-border-c";
    const inputClasses = "bg-modal-surface border border-surface-border text-modal-text-primary placeholder-modal-text-disabled focus:outline-none focus:ring-2";

    return (
        <div
            className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-modal-background/40 flex items-center justify-center z-200 p-4"
            onClick={onClose}
        >
            <div
                className={`relative w-full max-w-md max-h-[90vh] rounded-2xl overflow-hidden flex flex-col shadow-[0_18px_55px_rgba(0,0,0,0.45)] ${modalClasses}`}
                onClick={(e) => e.stopPropagation()}
            >
                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-modal-surface text-modal-text-secondary hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur"
                    disabled={isUpdating}
                    aria-label="Close modal"
                >
                    <FaTimes />
                </button>

                <div className="p-6 pb-3 flex items-center justify-between gap-4 pr-14">
                    <h2 className="text-2xl font-bold leading-tight">{t("edit-modal.title")}</h2>
                    <span
                        className="bg-server-color/20 border-server-color/50 text-server-color px-3 py-1 text-xs font-semibold rounded-full tracking-wide border"
                    >
                        ID: #{punishmentId}
                    </span>
                </div>

                <div className="px-6 pb-6 flex-1 overflow-y-auto">
                    <form onSubmit={handleSubmit} className="space-y-5">
                        {error && (
                            <div className="p-3 text-sm bg-modal-error border border-modal-error-border text-modal-text-error rounded-lg">
                                {error}
                            </div>
                        )}

                        <div className="space-y-3">
                            <div className="flex items-center justify-between">
                                <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                    {t("edit-modal.new-date.label")}
                                </label>
                                <label className="flex items-center gap-2 cursor-pointer text-sm text-modal-text-primary">
                                    <input
                                        type="checkbox"
                                        checked={isPermanent}
                                        onChange={(e) => setIsPermanent(e.target.checked)}
                                        className="accent-server-color rounded border-surface-border"
                                    />
                                    {t("edit-modal.permanent")}
                                </label>
                            </div>

                            {!isPermanent && (
                                <input
                                    type="datetime-local"
                                    className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} ring-2 ring-server-color/40`}
                                    value={newExpiry}
                                    onChange={(e) => setNewExpiry(e.target.value)}
                                    disabled={isUpdating}
                                    required={!isPermanent}
                                />
                            )}
                            {isPermanent && (
                                <div className={`w-full rounded-lg px-3 py-2 text-base italic text-modal-text-disabled border border-surface-border bg-modal-surface/50`}>
                                    {t("edit-modal.new-date.placeholder")}
                                </div>
                            )}
                        </div>

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                {t("edit-modal.new-reason.title")}
                            </label>
                            <textarea
                                className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} ring-2 ring-server-color/40`}
                                rows={4}
                                value={newReason}
                                onChange={(e) => setNewReason(e.target.value)}
                                required
                                disabled={isUpdating}
                                placeholder={t("edit-modal.new-reason.placeholder")}
                            />
                        </div>

                        <div className="flex justify-end gap-3 pt-2">
                            <button
                                type="button"
                                onClick={onClose}
                                className="px-4 py-2 rounded-xl bg-modal-surface text-modal-text-primary hover:bg-modal-surface-elevated transition duration-200 disabled:opacity-60"
                                disabled={isUpdating}
                            >
                                {t("edit-modal.buttons.cancel")}
                            </button>
                            <button
                                type="submit"
                                className="bg-server-color px-6 py-2 rounded-xl text-modal-text-primary font-semibold transition duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
                                disabled={isUpdating || !newReason.trim()}
                            >
                                {isUpdating ? t("edit-modal.buttons.confirm.saving") : t("edit-modal.buttons.confirm.save")}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default EditionModal;