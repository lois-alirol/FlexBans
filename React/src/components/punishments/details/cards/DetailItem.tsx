import React from 'react';

type Theme = 'dark' | 'light';

interface DetailItemProps {
    icon: React.ReactNode;
    label: string;
    value: string | null;
    accentColor: string;
    currentTheme: Theme;
}

const DetailItem: React.FC<DetailItemProps> = ({ icon, label, value, accentColor, currentTheme }) => {
    if (value === null) return null;

    return (
        <div className="flex items-center space-x-4 py-3 border-b border-gray-600/10 dark:border-gray-300/10 last:border-b-0">
            <div className="shrink-0 text-xl" style={{ color: accentColor }}>
                {icon}
            </div>
            <div className="grow">
                <p className={`text-sm font-medium ${currentTheme === 'dark' ? 'opacity-70' : 'text-gray-600'}`}>{label}</p>
                <p className="text-base font-semibold wrap-break-word">{value}</p>
            </div>
        </div>
    );
};

export default DetailItem;