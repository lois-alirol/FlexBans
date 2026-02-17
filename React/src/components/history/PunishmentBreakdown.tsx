import React from 'react';
import { FaChartBar } from 'react-icons/fa';

interface DistributionItem {
    label: string;
    count: number;
    color: string;
}

interface PunishmentBreakdownProps {
    distribution: DistributionItem[];
    totalPunishments: number;
    currentTheme: 'light' | 'dark';
    cardBg: string;
    borderColor: string;
}

const PunishmentBreakdown: React.FC<PunishmentBreakdownProps> = ({
                                                                     distribution,
                                                                     totalPunishments,
                                                                     currentTheme,
                                                                     cardBg,
                                                                     borderColor
                                                                 }) => {
    const total = totalPunishments || 1;

    return (
        <div className={`flex-grow rounded-2xl border ${borderColor} ${cardBg} p-6 flex flex-col shadow-sm`}>
            <h3 className="text-sm font-bold uppercase tracking-widest opacity-60 mb-6 flex items-center gap-2">
                <FaChartBar size={14} className="text-blue-500" />
                Punishment Breakdown
            </h3>
            <div className="space-y-4 flex-grow">
                {distribution.map((item) => {
                    const percent = Math.min(100, Math.round((item.count / total) * 100));
                    return (
                        <div key={item.label} className="space-y-2">
                            <div className="flex justify-between items-end">
                                <span className="text-sm font-bold">{item.label}</span>
                                <span className="text-xs font-mono opacity-50">{item.count} Total</span>
                            </div>
                            <div className={`h-2.5 w-full rounded-full ${currentTheme === 'dark' ? 'bg-gray-800' : 'bg-gray-100'}`}>
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