import React from 'react';

import MetricChip from '@components/common/MetricChip';

interface ChipData {
    title: string;
    value: number;
    icon: React.ReactNode;
    accent: string;
}

interface StatsCardsProps {
    chips: ChipData[];
}

const StatsCards: React.FC<StatsCardsProps> = ({ chips }) => {
    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {chips.map((chip) => (
                <MetricChip
                    key={chip.title}
                    title={chip.title}
                    value={chip.value}
                    icon={chip.icon}
                    accentClass={chip.accent}
                />
            ))}
        </div>
    );
};

export default StatsCards;