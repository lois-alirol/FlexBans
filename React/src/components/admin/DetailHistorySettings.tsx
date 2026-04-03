import React from "react";
import { useTranslation } from "react-i18next";

import Toggle from "@components/admin/Toggle";
import Slider from "@components/admin/Slider";

interface DetailSectionProps {
    details: {
        moderator: { enabled: boolean; maxPerPage: number };
        player: { enabled: boolean; maxPerPage: number };
        punishment: { enabled: boolean; revokeButton: boolean };
    };
    onUpdateDetails: (
        key: "moderator" | "player" | "punishment",
        patch: Record<string, any>
    ) => void;
}

const DetailHistorySettings: React.FC<DetailSectionProps> = ({
                                                                 details,
                                                                 onUpdateDetails,
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
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

            <div className="p-4 rounded-xl border bg-surface border-surface-border">
                {renderToggleRow(
                    t("admin.sections.detail-history-settings.moderator.title"),
                    details.moderator.enabled,
                    (v) => onUpdateDetails("moderator", { enabled: v })
                )}

                {renderMaxPerPage(
                    t("admin.sections.detail-history-settings.max-per-page"),
                    details.moderator.maxPerPage,
                    (v) => onUpdateDetails("moderator", { maxPerPage: v })
                )}
            </div>

            <div className="p-4 rounded-xl border bg-surface border-surface-border">
                {renderToggleRow(
                    t("admin.sections.detail-history-settings.player.title"),
                    details.player.enabled,
                    (v) => onUpdateDetails("player", { enabled: v })
                )}

                {renderMaxPerPage(
                    t("admin.sections.detail-history-settings.max-per-page"),
                    details.player.maxPerPage,
                    (v) => onUpdateDetails("player", { maxPerPage: v })
                )}
            </div>

            <div className="p-4 rounded-xl border bg-surface border-surface-border">
                {renderToggleRow(
                    t("admin.sections.detail-history-settings.punishment.title"),
                    details.punishment.enabled,
                    (v) => onUpdateDetails("punishment", { enabled: v })
                )}

                {renderToggleRow(
                    t("admin.sections.detail-history-settings.punishment.revoke-button"),
                    details.punishment.revokeButton,
                    (v) => onUpdateDetails("punishment", { revokeButton: v }),
                    t(
                        "admin.sections.detail-history-settings.punishment.revoke-helper"
                    )
                )}
            </div>

        </div>
    );
};

export default DetailHistorySettings;
