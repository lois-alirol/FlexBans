import React, { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { FaTimes, FaCheck, FaSpinner } from 'react-icons/fa';
import { useTranslation } from "react-i18next";

import { useNewPunishment } from '@hooks/useNewPunishment';

interface NewPunishmentModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    executorName: string;
}

interface PunishmentFormData {
    target: string;
    identity: string;
    punishmentType: 'ban' | 'mute' | 'kick' | 'warning' | '';
    silent: boolean;
    reason: string;
    duration: string;
    permanent: boolean;
}

const NewPunishmentModal: React.FC<NewPunishmentModalProps> = ({
                                                                   isOpen,
                                                                   onClose,
                                                                   onSuccess,
                                                                   executorName,
                                                               }) => {

    const { createPunishment, isCreating, error, success, resetState } = useNewPunishment();
    const { t } = useTranslation();

    const [formData, setFormData] = useState<PunishmentFormData>({
        target: '',
        identity: executorName,
        punishmentType: '',
        silent: false,
        reason: '',
        duration: '',
        permanent: false,
    });

    useEffect(() => {
        if (success) {
            const timer = setTimeout(() => {
                onSuccess();
            }, 800);
            return () => clearTimeout(timer);
        }
    }, [success, onSuccess]);

    useEffect(() => {
        if (success) {
            const timer = setTimeout(() => {
                onClose();
            }, 200);
            return () => clearTimeout(timer);
        }
    }, [success, onClose]);

    useEffect(() => {
        if (isOpen) {
            document.body.classList.add('overflow-hidden');
            resetState();
        } else {
            document.body.classList.remove('overflow-hidden');
            setFormData({
                target: '',
                identity: executorName,
                punishmentType: '',
                silent: false,
                reason: '',
                duration: '',
                permanent: false,
            });
        }
        return () => document.body.classList.remove('overflow-hidden');
    }, [isOpen, executorName, resetState]);

    if (!isOpen) return null;

    const modalClasses = 'bg-modal-background text-modal-text-primary border border-border-c';
    const inputClasses = 'bg-modal-surface border border-surface-border text-modal-text-primary placeholder-modal-text-disabled focus:outline-none focus:ring-2';
    const readOnlyInputClasses = 'bg-modal-surface border border-transparent cursor-not-allowed text-text-disabled';

    const handleChange = (
        e: ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
    ) => {
        const { name, value, type, checked } = e.target as HTMLInputElement;

        setFormData((prev) => {
            const next = { ...prev, [name]: type === 'checkbox' ? checked : value };

            if (name === 'punishmentType' && (value === 'kick' || value === 'warning')) {
                next.duration = '';
                next.permanent = false;
            }

            if (name === 'permanent' && checked) {
                next.duration = '';
            }

            return next;
        });
    };

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        resetState();

        const dataToSend = {
            target: formData.target,
            punishmentType: formData.punishmentType,
            silent: formData.silent,
            reason: formData.reason,
            duration:
                formData.punishmentType === 'kick' ||
                formData.punishmentType === 'warning' ||
                formData.permanent
                    ? null
                    : formData.duration,
            permanent:
                formData.punishmentType === 'kick' ||
                formData.punishmentType === 'warning'
                    ? false
                    : formData.permanent,
        };

        try {
            await createPunishment(dataToSend);
        } catch (submitError) {
            console.error('Submission failed:', submitError);
        }
    };

    const showDurationFields =
        formData.punishmentType !== 'kick' &&
        formData.punishmentType !== 'warning';

    const durationDisabled = formData.permanent || !showDurationFields;

    const PunishmentTypeOptions = ['ban', 'mute', 'kick', 'warning'];

    return (
        <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-modal-background/40 flex items-center justify-center z-200 p-4">
            <div className={`relative w-full max-w-xl max-h-[90vh] rounded-2xl overflow-hidden flex flex-col ${modalClasses}`}>
                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-modal-surface text-modal-text-secondary hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur"
                    disabled={isCreating}
                    aria-label="Close modal"
                >
                    <FaTimes />
                </button>

                <div className="p-8 pb-4 flex items-center justify-between gap-4 pr-16">

                    <h2 className="text-3xl font-bold leading-tight">
                        {t("create-modal.title")}
                    </h2>

                    <span
                        className="text-server-color bg-server-color/30 border border-server-color/70 px-3 py-1 text-xs font-semibold rounded-full tracking-wide uppercase"
                    >
                        {formData.punishmentType
                            ? formData.punishmentType.toUpperCase()
                            : t("create-modal.badge")}
                    </span>

                </div>

                <div className="px-8 pb-8 flex-1 overflow-y-auto">

                    {error && (
                        <div className="p-3 mb-5 rounded-lg bg-modal-error border border-modal-error-border text-modal-text-error">
                            {t("create-modal.error")} {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-6">

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                {t("create-modal.target")}
                            </label>

                            <input
                                type="text"
                                name="target"
                                value={formData.target}
                                onChange={handleChange}
                                className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} ring ring-server-color/40`}
                                required
                                disabled={isCreating || success}
                            />
                        </div>

                        <div className="space-y-2">

                            <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                {t("create-modal.executor")}
                            </label>

                            <input
                                type="text"
                                value={formData.identity}
                                readOnly
                                className={`w-full rounded-lg px-3 py-2 text-base ${readOnlyInputClasses}`}
                            />

                        </div>

                        <div className="space-y-2">

                            <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                {t("create-modal.reason")}
                            </label>

                            <textarea
                                name="reason"
                                value={formData.reason}
                                onChange={handleChange}
                                className={`w-full rounded-lg px-3 py-3 text-base transition min-h-25 resize-y ${inputClasses} ring ring-server-color/40`}
                                disabled={isCreating || success}
                            />

                        </div>

                        <div className="space-y-3">

                            <p className="text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                {t("create-modal.type")}
                            </p>

                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">

                                {PunishmentTypeOptions.map((type) => (
                                    <label
                                        key={type}
                                        className={`flex items-center gap-2 rounded-xl border px-3 py-2 text-sm capitalize transition ${
                                            formData.punishmentType === type
                                                ? 'bg-server-color/90 border-transparent text-modal-text-primary shadow-md'
                                                : 'border-surface-border text-modal-text-secondary hover:border-border-c'
                                        }`}
                                    >
                                        <input
                                            type="radio"
                                            name="punishmentType"
                                            value={type}
                                            checked={formData.punishmentType === type}
                                            onChange={handleChange}
                                            className="accent-server-color h-4 w-4"
                                            required
                                            disabled={isCreating || success}
                                        />
                                        {t(`punishments.${type}`)}
                                    </label>
                                ))}

                            </div>

                        </div>

                        <div className="flex items-center justify-between rounded-xl border border-surface-border bg-modal-surface px-3 py-3 backdrop-blur">

                            <div>
                                <p className="text-sm font-semibold text-modal-text-primary">
                                    {t("create-modal.silent.title")}
                                </p>

                                <p className="text-xs text-modal-text-secondary">
                                    {t("create-modal.silent.description")}
                                </p>
                            </div>

                            <input
                                type="checkbox"
                                name="silent"
                                checked={formData.silent}
                                onChange={handleChange}
                                className="accent-server-color h-4 w-4"
                                disabled={isCreating || success}
                            />

                        </div>

                        {showDurationFields && (
                            <>
                                <div className="space-y-2">

                                    <label className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                                        {t("create-modal.duration.label")}
                                    </label>

                                    <input
                                        type="text"
                                        name="duration"
                                        value={formData.duration}
                                        onChange={handleChange}
                                        placeholder={t("create-modal.duration.placeholder")}
                                        className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} ring ring-server-color/40`}
                                        disabled={durationDisabled || isCreating || success}
                                        required={!durationDisabled}
                                    />

                                </div>

                                <div className="flex items-center justify-between rounded-xl border border-surface-border bg-modal-surface px-3 py-3 backdrop-blur">

                                    <div>
                                        <p className="text-sm font-semibold text-modal-text-primary">
                                            {t("create-modal.permanent.title")}
                                        </p>

                                        <p className="text-xs text-modal-text-secondary">
                                            {t("create-modal.permanent.description")}
                                        </p>
                                    </div>

                                    <input
                                        type="checkbox"
                                        name="permanent"
                                        checked={formData.permanent}
                                        onChange={handleChange}
                                        className="accent-server-color h-4 w-4"
                                        disabled={isCreating || success}
                                    />

                                </div>
                            </>
                        )}

                        <button
                            type="submit"
                            className="bg-server-color w-full text-modal-text-primary font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0 disabled:opacity-60 disabled:cursor-not-allowed"
                            disabled={isCreating || success}
                        >
                            {isCreating ? (
                                <>
                                    <FaSpinner className="inline mr-2 animate-spin" />
                                    {t("create-modal.buttons.submitting")}
                                </>
                            ) : (
                                <>
                                    <FaCheck className="inline mr-2" />
                                    {t("create-modal.buttons.submit")}
                                </>
                            )}
                        </button>

                    </form>
                </div>
            </div>
        </div>
    );
};

export default NewPunishmentModal;