import React from "react";
import { FaBolt, FaUndo } from "react-icons/fa";
import { useTranslation } from "react-i18next";

import type { ServerConfig } from "@/types/config";

import Toggle from "@components/admin/Toggle";
import Slider from "@components/admin/Slider";

type PunishmentKey = keyof ServerConfig["punishments"];

interface Props {
    punishments: ServerConfig["punishments"];
    features: { allowExecution: boolean; allowRevocation: boolean };
    onUpdatePunishment: (
        key: PunishmentKey,
        patch: Partial<ServerConfig["punishments"][PunishmentKey]>
    ) => void;
    onUpdateFeature: (
        key: "allowExecution" | "allowRevocation",
        value: boolean
    ) => void;
}

const PunishmentSettings: React.FC<Props> = ({
                                                 punishments,
                                                 features,
                                                 onUpdatePunishment,
                                                 onUpdateFeature,
                                             }) => {
    const { t } = useTranslation();

    const renderToggleRow = (
        label: string,
        checked: boolean,
        onChange: (v: boolean) => void,
        helper?: string
    ) => (
        <div className="flex items-center justify-between py-3">
            <div>
                <p className="font-semibold">{label}</p>
                {helper && <p className="text-sm text-text-secondary">{helper}</p>}
            </div>
            <Toggle checked={checked} onChange={onChange} />
        </div>
    );

    const renderMaxPerPage = (
        label: string,
        value: number,
        onChange: (v: number) => void
    ) => (
        <div className="space-y-2">
            <div className="flex items-center justify-between">
                <p className="font-semibold">{label}</p>
                <span className="text-xs text-text-secondary">5 - 100</span>
            </div>
            <Slider
                value={value}
                min={5}
                max={100}
                step={5}
                onChange={onChange}
            />
        </div>
    );

    return (
        <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {(["bans", "mutes", "warnings", "kicks"] as const).map((key) => {
                    const label = t(`admin.sections.punishment-settings.types.${key}`);
                    const entry = punishments[key];

                    return (
                        <div
                            key={key}
                            className="p-4 rounded-xl border bg-surface border-surface-border"
                        >
                            {renderToggleRow(
                                t("admin.sections.punishment-settings.enabled", {
                                    type: label,
                                }),
                                entry.enabled,
                                (v) => onUpdatePunishment(key, { enabled: v }),
                                t("admin.sections.punishment-settings.toggle-helper", {
                                    type: label.toLowerCase(),
                                })
                            )}

                            {renderMaxPerPage(
                                t("admin.sections.punishment-settings.max-per-page"),
                                entry.maxPerPage,
                                (v) => onUpdatePunishment(key, { maxPerPage: v })
                            )}
                        </div>
                    );
                })}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-4">
                <div className="p-4 rounded-xl border bg-surface border-surface-border">
                    <div className="space-y-3">

                        <div className="flex items-start gap-3">
                            <div
                                className="bg-server-color flex h-10 w-10 items-center justify-center rounded-lg text-text-primary shadow-sm"
                            >
                                <FaBolt />
                            </div>

                            <div className="flex-1">
                                <div className="flex items-center justify-between gap-3">
                                    <p className="font-semibold">
                                        {t(
                                            "admin.sections.punishment-settings.execution.title"
                                        )}
                                    </p>

                                    <Toggle
                                        checked={features.allowExecution}
                                        onChange={(v) => onUpdateFeature("allowExecution", v)}
                                    />
                                </div>

                                <p className="text-sm text-text-secondary">
                                    {t(
                                        "admin.sections.punishment-settings.execution.description"
                                    )}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 border-t border-surface-border pt-3">
                            <div
                                className="bg-server-color flex h-10 w-10 items-center justify-center rounded-lg text-text-primary shadow-sm"
                            >
                                <FaUndo />
                            </div>

                            <div className="flex-1">
                                <div className="flex items-center justify-between gap-3">
                                    <p className="font-semibold">
                                        {t(
                                            "admin.sections.punishment-settings.revocation.title"
                                        )}
                                    </p>

                                    <Toggle
                                        checked={features.allowRevocation}
                                        onChange={(v) => onUpdateFeature("allowRevocation", v)}
                                    />
                                </div>

                                <p className="text-sm text-text-secondary">
                                    {t(
                                        "admin.sections.punishment-settings.revocation.description"
                                    )}
                                </p>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </>
    );
};

export default PunishmentSettings;
