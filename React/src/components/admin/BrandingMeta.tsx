import React from 'react';
import { FaImage, FaLink, FaAlignLeft } from 'react-icons/fa';

import type { BrandingForm } from '@/types/admin';
import {useTranslation} from "react-i18next";

interface Props {
    branding: BrandingForm;
    onChange: (key: keyof BrandingForm, value: string) => void;
}

const BrandingMeta: React.FC<Props> = ({ branding, onChange }) => {
    const { t } = useTranslation();

    const inputClass =
        "mt-2 w-full p-3 rounded-xl border bg-surface-elevated border-surface-border text-text-primary focus:ring-2 focus:ring-server-color outline-none";

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
                <div>
                    <label className="text-sm font-semibold flex items-center gap-2">
                        {t("admin.sections.branding-meta.fields.server-name")}
                    </label>
                    <input
                        type="text"
                        value={branding.serverName}
                        onChange={(e) => onChange('serverName', e.target.value)}
                        placeholder={t("admin.sections.branding-meta.fields.server-name-placeholder")}
                        className={inputClass}
                    />
                </div>

                <div className="space-y-2">
                    <label className="text-sm font-semibold flex items-center gap-2">
                        {t("admin.sections.branding-meta.fields.primary-color")}
                    </label>
                    <div className="flex items-center gap-3">
                        <label className="relative inline-block">
                            <div
                                className="w-12 h-12 rounded-lg border shadow-sm border-surface-border"
                                style={{ backgroundColor: branding.primaryColor }}
                            />
                            <input
                                type="color"
                                value={branding.primaryColor}
                                onChange={(e) => onChange('primaryColor', e.target.value)}
                                onInput={(e) =>
                                    onChange(
                                        'primaryColor',
                                        (e.target as HTMLInputElement).value
                                    )
                                }
                                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                                aria-label="Pick primary color"
                            />
                        </label>

                        <input
                            type="text"
                            value={branding.primaryColor}
                            onChange={(e) => onChange('primaryColor', e.target.value)}
                            className={inputClass}
                        />
                    </div>
                </div>

                <div className="space-y-2">
                    <label className="text-sm font-semibold flex items-center gap-2">
                        {t("admin.sections.branding-meta.fields.secondary-color")}
                    </label>
                    <div className="flex items-center gap-3">
                        <label className="relative inline-block">
                            <div
                                className="w-12 h-12 rounded-lg border shadow-sm border-surface-border"
                                style={{ backgroundColor: branding.secondaryColor }}
                            />
                            <input
                                type="color"
                                value={branding.secondaryColor}
                                onChange={(e) => onChange('secondaryColor', e.target.value)}
                                onInput={(e) =>
                                    onChange(
                                        'secondaryColor',
                                        (e.target as HTMLInputElement).value
                                    )
                                }
                                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                                aria-label="Pick secondary color"
                            />
                        </label>

                        <input
                            type="text"
                            value={branding.secondaryColor}
                            onChange={(e) => onChange('secondaryColor', e.target.value)}
                            className={inputClass}
                        />
                    </div>
                </div>
            </div>

            <div className="space-y-4">
                <div>
                    <label className="text-sm font-semibold flex items-center gap-2">
                        <FaImage /> {t("admin.sections.branding-meta.fields.logo-url")}
                    </label>
                    <input
                        type="text"
                        value={branding.logo}
                        onChange={(e) => onChange('logo', e.target.value)}
                        placeholder="https://example.com/logo.png"
                        className={inputClass}
                    />
                </div>

                <div>
                    <label className="text-sm font-semibold flex items-center gap-2">
                        <FaLink /> {t("admin.sections.branding-meta.fields.favicon-url")}
                    </label>
                    <input
                        type="text"
                        value={branding.favicon}
                        onChange={(e) => onChange('favicon', e.target.value)}
                        placeholder="https://example.com/favicon.ico"
                        className={inputClass}
                    />
                </div>

                <div>
                    <label className="text-sm font-semibold flex items-center gap-2">
                        <FaAlignLeft /> {t("admin.sections.branding-meta.fields.description")}
                    </label>
                    <textarea
                        value={branding.description}
                        onChange={(e) => onChange('description', e.target.value)}
                        placeholder={t("admin.sections.branding-meta.fields.description-placeholder")}
                        rows={3}
                        className={inputClass + " resize-none"}
                    />
                </div>
            </div>
        </div>
    );
};

export default BrandingMeta;