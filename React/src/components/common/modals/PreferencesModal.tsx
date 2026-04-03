import React, { useState, useEffect } from 'react';
import { FaTimes, FaCheck, FaSpinner, FaGlobe, FaPalette, FaChevronDown } from 'react-icons/fa';
import {useTranslation} from "react-i18next";
import {useTheme} from "@hooks/useTheme.ts";

const MiniPreview = ({ themeId }: { themeId: string; }) => {
    const getStyles = () => {
        switch (themeId) {
            case 'light':
                return { bg: 'bg-[#ffffff]', card: 'bg-[#f9fafb]', bar: 'bg-[#e5e7eb]', side: 'bg-[#f9fafb]' };
            case 'midnight':
                return { bg: 'bg-[#020617]', card: 'bg-[#0f172a]', bar: 'bg-[#334155]', side: 'bg-[#02040a]' };
            case 'quartz':
                return { bg: 'bg-[#fffbfb]', card: 'bg-[#fdf2f2]', bar: 'bg-[#f1e4e4]', side: 'bg-[#fdf2f2]' };
            case 'dark':
            default:
                return { bg: 'bg-[#1c1c1c]', card: 'bg-[#282828]', bar: 'bg-[#3d3d3d]', side: 'bg-[#151515]' };
        }
    };

    const s = getStyles();

    return (
        <div className={`w-full h-24 rounded-lg mb-3 overflow-hidden flex border border-white/5 shadow-inner transition-transform group-hover:scale-[1.02] ${s.bg}`}>
            <div className={`w-4 h-full ${s.side} p-1.5 space-y-1.5`}>
                <div className="bg-server-color w-full h-1.5 rounded-full opacity-40" />
                <div className="w-full h-1.5 rounded-full bg-white/5" />
            </div>
            <div className="flex-1 p-3 space-y-2">
                <div className="flex items-center gap-1.5">
                    <div className="bg-server-color w-3 h-3 rounded-full" />
                    <div className={`h-1.5 w-12 rounded-full ${s.bar}`} />
                </div>
                <div className={`w-full h-12 rounded-md ${s.card} p-2 space-y-1.5`}>
                    <div className={`h-1.5 w-full rounded-full ${s.bar}`} />
                    <div className={`h-1.5 w-2/3 rounded-full ${s.bar}`} />
                </div>
            </div>
        </div>
    );
};

const PreferencesModal: React.FC<any> = ({ isOpen, onClose, initialSettings = { theme: 'dark', language: 'en' }, onApply }) => {
    const [isSaving, setIsSaving] = useState(false);
    const [success, setSuccess] = useState(false);
    const [formData, setFormData] = useState(initialSettings);

    const { t, i18n } = useTranslation();
    const currentLanguage = i18n.language;

    const { theme, setTheme } = useTheme();

    useEffect(() => {
        const storedTheme = localStorage.getItem('app-theme');
        const storedLanguage = localStorage.getItem('app-language');

        setFormData((prev: { theme: any; language: any; }) => ({
            theme: storedTheme ?? prev.theme,
            language: storedLanguage ?? prev.language,
        }));
    }, []);

    useEffect(() => {
        if (isOpen) {
            document.body.classList.add('overflow-hidden');
        } else {
            document.body.classList.remove('overflow-hidden');
            setSuccess(false);
        }
        return () => document.body.classList.remove('overflow-hidden');
    }, [isOpen]);

    if (!isOpen) return null;

    const modalClasses = 'bg-modal-background text-modal-text-primary border border-border-c'

    const themes = [
        { key: "light", label: t("preferences-modal.themes.light")},
        { key: "dark", label: t("preferences-modal.themes.dark")},
        { key: "quartz", label: t("preferences-modal.themes.quartz")},
        { key: "midnight", label: t("preferences-modal.themes.midnight")}
    ];

    const languages = [
        { code: 'en-US', label: 'English (US)' },
        { code: 'es-ES', label: 'Español' },
        { code: 'fr-FR', label: 'Français' },
        { code: 'de-DE', label: 'Deutsch' }
    ];

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSaving(true);

        try {
            if (theme != formData.theme) setTheme(formData.theme);
            if (currentLanguage !== formData.language) {
                localStorage.setItem("app-language", formData.language);
                await i18n.changeLanguage(formData.language);
            }

            setIsSaving(false);
            setSuccess(true);

            onApply();

            setTimeout(() => {
                onClose();
            }, 200);

        } catch (error) {
            console.error('Failed to save preferences:', error);
            setIsSaving(false);
        }
    };

    return (
        <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-modal-background/40 flex items-center justify-center z-200 p-4 text-left">
            <div
                className={`relative w-full max-w-xl max-h-[90vh] rounded-2xl overflow-hidden flex flex-col ${modalClasses}`}
            >
                <button
                    onClick={onClose}
                    className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-modal-surface text-modal-text-secondary hover:text-modal-text-error hover:bg-modal-error transition duration-200 backdrop-blur"
                    disabled={isSaving || success}
                >
                    <FaTimes />
                </button>

                <div className="p-8 pb-4 flex items-center justify-between gap-4 pr-16">
                    <div>
                        <h2 className="text-3xl font-bold leading-tight">{t("preferences-modal.title")}</h2>
                        <p className="text-modal-text-secondary mt-1 text-sm font-medium">{t("preferences-modal.description")}</p>
                    </div>
                </div>

                <div className="px-8 pb-8 flex-1 overflow-y-auto">
                    <form className="space-y-8" onSubmit={handleSubmit}>

                        <div className="space-y-3">
                            <label className="flex text-sm font-semibold uppercase tracking-wide text-modal-text-secondary items-center gap-2">
                                <FaPalette /> {t("preferences-modal.themes.title")}
                            </label>

                            <div className="grid grid-cols-2 gap-4">
                                {themes.map((t) => (
                                    <label key={t.key} className="group cursor-pointer">
                                        <input
                                            type="radio"
                                            name="theme"
                                            value={t.key}
                                            checked={formData.theme === t.key}
                                            onChange={(e) => setFormData({...formData, theme: e.target.value})}
                                            className="hidden"
                                            disabled={isSaving || success}
                                        />
                                        <div
                                            className={`
                                                p-4 rounded-2xl border transition-all duration-300
                                                ${formData.theme === t.key
                                                ? 'bg-server-color/90 border-transparent shadow-md'
                                                : 'border-surface-border bg-modal-surface hover:border-border-active'}
                                            `}
                                        >
                                            <MiniPreview themeId={t.key} />

                                            <div className="flex items-center justify-between px-1">
                                                <span className={`text-sm font-bold text-modal-text-primary`}>
                                                    {t.label}
                                                </span>
                                                {formData.theme === t.key && <FaCheck className="text-sm text-modal-text-primary" />}
                                            </div>
                                        </div>
                                    </label>
                                ))}
                            </div>
                        </div>

                        <div className="space-y-3">
                            <label className="flex text-sm font-semibold uppercase tracking-wide text-modal-text-secondary items-center gap-2">
                                <FaGlobe /> {t("preferences-modal.languages.title")}
                            </label>

                            <div className="relative group">
                                <select
                                    value={formData.language}
                                    onChange={(e) => setFormData({ ...formData, language: e.target.value })}
                                    disabled={isSaving || success}
                                    className={`
                                        w-full appearance-none px-5 py-4 rounded-xl border outline-none
                                        transition-all duration-200 cursor-pointer font-medium
                                        bg-modal-surface border-surface-border text-modal-text-primary hover:border-border-active
                                    `}
                                    style={{
                                        boxShadow: formData.language ? `0 0 0 0 transparent` : undefined
                                    }}
                                >
                                    {languages.map((lang) => (
                                        <option
                                            key={lang.code}
                                            value={lang.code}
                                            className={"bg-modal-surface text-modal-text-primary"}
                                        >
                                            {lang.label}
                                        </option>
                                    ))}
                                </select>

                                <div className="absolute inset-y-0 right-5 flex items-center pointer-events-none text-modal-text-secondary group-hover:text-modal-text-primary transition-colors">
                                    <FaChevronDown className="text-xs" />
                                </div>
                            </div>
                        </div>

                        <div className="pt-2">
                            <button
                                type="submit"
                                className="bg-server-color w-full text-modal-text-primary font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0 disabled:opacity-60 disabled:cursor-not-allowed"
                                disabled={isSaving || success}
                            >
                                {isSaving ? (
                                    <>
                                        <FaSpinner className="inline mr-2 animate-spin" /> {t("preferences-modal.button.saving")}
                                    </>
                                ) : (
                                    <>
                                        <FaCheck className="inline mr-2" /> {t("preferences-modal.button.save")}
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default PreferencesModal;