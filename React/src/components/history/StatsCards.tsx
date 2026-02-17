import React from 'react';
import MetricChip from '../common/MetricChip';

interface ChipData {
    title: string;
    value: number;
    icon: React.ReactNode;
    accent: string;
}

interface StatsCardsProps {
    chips: ChipData[];
    currentTheme: 'light' | 'dark';
}

const StatsCards: React.FC<StatsCardsProps> = ({ chips, currentTheme }) => {
    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {chips.map((chip) => (
                <MetricChip
                    key={chip.title}
                    title={chip.title}
                    value={chip.value}
                    icon={chip.icon}
                    currentTheme={currentTheme}
                    accentClass={chip.accent}
                />
            ))}
        </div>
    );
};

export default StatsCards;