import React from 'react';
import { FaChartBar } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

interface DistributionItem {
    label: string;
    count: number;
    color: string;
}

interface PunishmentBreakdownProps {
    distribution: DistributionItem[];
    totalPunishments: number;
    cardBg: string;
}

const PunishmentBreakdown: React.FC<PunishmentBreakdownProps> = ({
                                                                     distribution,
                                                                     totalPunishments,
                                                                     cardBg,
                                                                 }) => {
    const total = totalPunishments || 1;
    const { t } = useTranslation();

    return (
        <div className={`grow rounded-2xl border border-surface-border ${cardBg} p-6 flex flex-col`}>
            <h3 className="text-sm text-text-secondary font-bold uppercase tracking-widest mb-6 flex items-center gap-2">
                <FaChartBar size={14} />
                {t("history.breakdown.title")}
            </h3>
            <div className="space-y-4 grow">
                {distribution.map((item) => {
                    const percent = Math.min(100, Math.round((item.count / total) * 100));
                    return (
                        <div key={item.label} className="space-y-2">
                            <div className="flex justify-between items-end">
                                <span className="text-sm font-bold">{item.label}</span>
                                <span className="text-xs font-mono opacity-50">
                                    {t("history.breakdown.count", {
                                            count: item.count
                                    })}
                                </span>
                            </div>
                            <div className={`h-2.5 w-full rounded-full bg-surface-elevated`}>
                                <div
                                    className={`h-full rounded-full transition-all duration-1000 ${item.color}`}
                                    style={{ width: `${percent}%` }}
                                />
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default PunishmentBreakdown;