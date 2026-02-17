import React, { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { FaTimes, FaCheck, FaSpinner } from 'react-icons/fa';
import {useServerConfig} from "../../../hooks/useServerConfig.ts";
import {useNewPunishment} from "../../../hooks/useNewPunishment.ts";

interface NewPunishmentModalProps {
    isOpen: boolean;
    onClose: () => void;
    executorName: string;
    currentTheme: 'dark' | 'light';
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
                                                                   executorName,
                                                                   currentTheme,
                                                               }) => {
    const { serverConfig } = useServerConfig();
    const { createPunishment, isCreating, error, success, resetState } = useNewPunishment();

    const [formData, setFormData] = useState<PunishmentFormData>({
        target: '',
        identity: executorName,
        punishmentType: '',
        silent: false,
        reason: '',
        duration: '',
        permanent: false,
    });

    // Handle auto-close after success
    useEffect(() => {
        if (success) {
            const timer = setTimeout(() => {
                onClose();
            }, 1800); // 1.8 seconds delay to show the "Done" state
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

    const isDark = currentTheme === 'dark';
    const modalClasses = isDark
        ? 'bg-[#151515e6] text-white border border-white/10'
        : 'bg-white/90 text-gray-900 border border-black/5';
    const inputClasses = isDark
        ? 'bg-white/5 border border-white/10 text-white placeholder-white/60 focus:ring-2 focus:ring-green-500/70'
        : 'bg-gray-50 border border-gray-200 text-gray-900 placeholder-gray-500 focus:ring-2 focus:ring-green-500/70';
    const readOnlyInputClasses =
        'bg-white/10 dark:bg-white/5 border border-transparent cursor-not-allowed text-white/80 dark:text-white/80';

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
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
                formData.punishmentType === 'kick' || formData.punishmentType === 'warning'
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
        formData.punishmentType !== 'kick' && formData.punishmentType !== 'warning';
    const durationDisabled = formData.permanent || !showDurationFields;
    const PunishmentTypeOptions = ['ban', 'mute', 'kick', 'warning'];

    return (
        <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-black/40 flex items-center justify-center z-[200] p-4">
            <div
                className={`relative w-full max-w-xl max-h-[90vh] rounded-2xl shadow-[0_18px_55px_rgba(0,0,0,0.45)] overflow-hidden flex flex-col ${modalClasses}`}
                style={{
                    backgroundImage: isDark
                        ? 'linear-gradient(160deg, rgba(28,28,28,0.95), rgba(18,18,18,0.9))'
                        : 'linear-gradient(160deg, rgba(255,255,255,0.95), rgba(245,245,245,0.9))',
                }}
            >
                {/* BIG SUCCESS OVERLAY */}
                {success && (
                    <div className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-black/60 backdrop-blur-md animate-in fade-in zoom-in duration-300">
                        <div
                            className="w-24 h-24 rounded-full flex items-center justify-center mb-4 animate-bounce"
                            style={{ backgroundColor: serverConfig.serverColor }}
                        >
                            <FaCheck className="text-white text-5xl" />
                        </div>
                        <h2 className="text-4xl font-black text-white tracking-tighter uppercase italic">
                            Executed!
                        </h2>
                        <p className="text-white/70 mt-2 font-medium">Closing modal...</p>
                    </div>
                )}

                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-white/6 text-gray-400 hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur"
                    disabled={isCreating}
                    aria-label="Close modal"
                >
                    <FaTimes />
                </button>

                <div className="p-8 pb-4 flex items-center justify-between gap-4 pr-16">
                    <h2 className="text-3xl font-bold leading-tight">New Punishment</h2>
                    <span
                        className="px-3 py-1 text-xs font-semibold rounded-full tracking-wide uppercase"
                        style={{
                            color: serverConfig.serverColor,
                            backgroundColor: `${serverConfig.serverColor}22`,
                            border: `1px solid ${serverConfig.serverColor}44`,
                        }}
                    >
            {formData.punishmentType ? formData.punishmentType.toUpperCase() : 'SELECT TYPE'}
          </span>
                </div>

                <div className="px-8 pb-8 flex-1 overflow-y-auto">
                    {error && (
                        <div className="p-3 mb-5 rounded-lg bg-red-900/70 text-white font-medium border border-red-500/40">
                            Error: {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div className="space-y-2">
                            <label htmlFor="target" className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                Player to Punish
                            </label>
                            <input
                                type="text"
                                id="target"
                                name="target"
                                value={formData.target}
                                onChange={handleChange}
                                className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} focus:-translate-y-[1px]`}
                                required
                                disabled={isCreating || success}
                            />
                        </div>

                        <div className="space-y-2">
                            <label htmlFor="executor" className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                Executor Name
                            </label>
                            <input
                                type="text"
                                id="executor"
                                name="identity"
                                value={formData.identity}
                                readOnly
                                className={`w-full rounded-lg px-3 py-2 text-base ${readOnlyInputClasses}`}
                                required
                            />
                        </div>

                        <div className="space-y-3">
                            <p className="text-sm font-semibold uppercase tracking-wide text-gray-400">Punishment Type</p>
                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                                {PunishmentTypeOptions.map((type) => (
                                    <label
                                        key={type}
                                        className={`flex items-center gap-2 rounded-xl border px-3 py-2 text-sm capitalize transition ${
                                            formData.punishmentType === type
                                                ? 'border-transparent text-white shadow-md'
                                                : 'border-white/10 text-gray-300 hover:border-white/30'
                                        }`}
                                        style={
                                            formData.punishmentType === type
                                                ? {
                                                    backgroundColor: `${serverConfig.serverColor}dd`,
                                                    boxShadow: `0 10px 30px ${serverConfig.serverColor}55`,
                                                }
                                                : {}
                                        }
                                    >
                                        <input
                                            type="radio"
                                            name="punishmentType"
                                            value={type}
                                            checked={formData.punishmentType === type}
                                            onChange={handleChange}
                                            className="h-4 w-4"
                                            style={{ accentColor: serverConfig.serverColor }}
                                            required
                                            disabled={isCreating || success}
                                        />
                                        {type}
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/6 px-3 py-3 backdrop-blur">
                            <div>
                                <p className="text-sm font-semibold text-gray-200">Silent Mode</p>
                                <p className="text-xs text-gray-400">Don't notify everyone when executed</p>
                            </div>
                            <label className="inline-flex items-center gap-2 text-sm font-medium">
                                <input
                                    type="checkbox"
                                    name="silent"
                                    checked={formData.silent}
                                    onChange={handleChange}
                                    className="h-4 w-4"
                                    style={{ accentColor: serverConfig.serverColor }}
                                    disabled={isCreating || success}
                                />
                            </label>
                        </div>

                        <div className="space-y-2">
                            <label htmlFor="reason" className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                Reason
                            </label>
                            <input
                                type="text"
                                id="reason"
                                name="reason"
                                value={formData.reason}
                                onChange={handleChange}
                                className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} focus:-translate-y-[1px]`}
                                required
                                disabled={isCreating || success}
                            />
                        </div>

                        {showDurationFields && (
                            <div className="space-y-3 transition-all duration-300">
                                <label htmlFor="duration" className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                                    Duration (e.g., 1h, 1d, 3w)
                                </label>
                                <input
                                    type="text"
                                    id="duration"
                                    name="duration"
                                    value={formData.duration}
                                    onChange={handleChange}
                                    className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses} ${
                                        durationDisabled ? 'opacity-60 cursor-not-allowed' : 'focus:-translate-y-[1px]'
                                    }`}
                                    placeholder="e.g., 1h, 1d"
                                    disabled={durationDisabled || isCreating || success}
                                    required={!durationDisabled && !success}
                                />

                                <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/6 px-3 py-3 backdrop-blur">
                                    <div>
                                        <p className="text-sm font-semibold text-gray-200">Permanent</p>
                                        <p className="text-xs text-gray-400">Indefinite action; disables duration</p>
                                    </div>
                                    <label className="inline-flex items-center gap-2 text-sm font-medium">
                                        <input
                                            type="checkbox"
                                            id="permanent"
                                            name="permanent"
                                            checked={formData.permanent}
                                            onChange={handleChange}
                                            className="h-4 w-4"
                                            style={{ accentColor: serverConfig.serverColor }}
                                            disabled={isCreating || success}
                                        />
                                    </label>
                                </div>
                            </div>
                        )}

                        <button
                            type="submit"
                            className="w-full text-white font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-[1px] active:translate-y-0 disabled:opacity-60 disabled:cursor-not-allowed"
                            style={{
                                backgroundColor: serverConfig.serverColor,
                                boxShadow: `0 10px 25px ${serverConfig.serverColor}55`,
                            }}
                            disabled={isCreating || success}
                        >
                            {isCreating ? (
                                <>
                                    <FaSpinner className="inline mr-2 animate-spin" /> Submitting...
                                </>
                            ) : (
                                <>
                                    <FaCheck className="inline mr-2" /> Submit Punishment
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