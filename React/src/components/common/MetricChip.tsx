import React from 'react';

interface MetricChipProps {
    title: string;
    value: number | string;
    icon: React.ReactNode;
    accentClass?: string;
}

const MetricChip: React.FC<MetricChipProps> = ({
                                                   title,
                                                   value,
                                                   icon,
                                                   accentClass = '',
                                               }) => {
    const baseBg = 'bg-surface border-surface-border';
    const textColor = 'text-text-secondary';
    const valueColor = 'text-text-primary';

    return (
        <div className={`flex items-center justify-between p-5 rounded-2xl border ${baseBg} transition-all duration-300 hover:shadow-lg group shadow-sm overflow-hidden relative`}>
            <div className="flex flex-col relative z-10">
                <span className={`text-[10px] font-bold uppercase tracking-[0.15em] ${textColor}`}>{title}</span>
                <span className={`text-2xl font-black mt-0.5 ${valueColor}`}>{value}</span>
            </div>
            <div className={`flex items-center justify-center w-11 h-11 rounded-xl ${accentClass} shadow-sm relative z-10 transition-transform duration-300 group-hover:-translate-y-1`}>
                <div className="text-xl">
                    {icon}
                </div>
            </div>
        </div>
    );
};

export default MetricChip;