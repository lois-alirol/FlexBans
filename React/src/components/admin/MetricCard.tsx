import React from 'react';

interface MetricCardProps {
    icon: React.ReactNode;
    label: string;
    value: string | number;
}

const MetricCard: React.FC<MetricCardProps> = ({ icon, label, value }) => (
    <div className={`p-5 rounded-2xl shadow-lg border bg-surface border-surface-border`}>
        <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
                <div className="text-server-color text-2xl">
                    {icon}
                </div>
                <p className={`text-sm font-semibold text-text-secondary`}>{label}</p>
            </div>
            <p className="text-2xl font-extrabold">{value}</p>
        </div>
    </div>
);

export default MetricCard;