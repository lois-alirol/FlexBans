import React from 'react';
import type { Theme } from "../../types/admin.tsx";

interface MetricCardProps {
    icon: React.ReactNode;
    label: string;
    value: string | number;
    accent: string;
    theme: Theme;
}

const MetricCard: React.FC<MetricCardProps> = ({ icon, label, value, accent, theme }) => (
    <div className={`p-5 rounded-2xl shadow-lg border ${theme === 'dark' ? 'bg-[#242424] border-gray-700' : 'bg-white border-gray-200'}`}>
        <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
                <div className="text-2xl" style={{ color: accent }}>
                    {icon}
                </div>
                <p className={`text-sm font-semibold ${theme === 'dark' ? 'text-gray-300' : 'text-gray-600'}`}>{label}</p>
            </div>
            <p className="text-2xl font-extrabold">{value}</p>
        </div>
    </div>
);

export default MetricCard;