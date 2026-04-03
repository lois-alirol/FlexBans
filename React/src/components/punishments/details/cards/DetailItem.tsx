import React from 'react';

interface DetailItemProps {
    icon: React.ReactNode;
    label: string;
    value: string | null;
}

const DetailItem: React.FC<DetailItemProps> = ({ icon, label, value }) => {
    if (value === null) return null;

    return (
        <div className="flex items-center space-x-4 py-3 border-b border-surface-border last:border-b-0">
            <div className="shrink-0 text-xl text-server-color">
                {icon}
            </div>
            <div className="grow">
                <p className={`text-sm font-medium text-text-secondary`}>{label}</p>
                <p className="text-base font-semibold wrap-break-word">{value}</p>
            </div>
        </div>
    );
};

export default DetailItem;