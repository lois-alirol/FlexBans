import React from 'react';

type Theme = 'light' | 'dark';

interface MetricChipProps {
    title: string;
    value: number | string;
    icon: React.ReactNode;
    currentTheme: Theme;
    accentClass?: string;
}

const MetricChip: React.FC<MetricChipProps> = ({
                                                   title,
                                                   value,
                                                   icon,
                                                   currentTheme,
                                                   accentClass = '',
                                               }) => {
    const baseBg = currentTheme === 'dark' ? 'bg-[#333]' : 'bg-gray-100';
    const textColor = currentTheme === 'dark' ? 'text-gray-400' : 'text-gray-500';
    const valueColor = currentTheme === 'dark' ? 'text-gray-100' : 'text-gray-900';

    return (
        <div className={`flex items-center justify-between p-5 rounded-2xl ${baseBg} transition-all duration-300 hover:shadow-lg group shadow-sm overflow-hidden relative`}>
            <div className={`absolute -right-2 -bottom-2 opacity-10 transition-transform group-hover:scale-125 duration-500 ${textColor}`}>
                {React.cloneElement(icon as React.ReactElement)}
            </div>

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