import React from 'react';
import { FaCalendarAlt, FaClock } from 'react-icons/fa';
import DetailCard from './DetailCard';
import DetailItem from './DetailItem';

type Theme = 'dark' | 'light';

interface TimeInformationCardProps {
    executionDate: string;
    duration?: string | null;
    expirationDate?: string | null;
    serverColor: string;
    currentTheme: Theme;
    isKickType: boolean;
}

const TimeInformationCard: React.FC<TimeInformationCardProps> = ({
                                                                     executionDate,
                                                                     duration,
                                                                     expirationDate,
                                                                     serverColor,
                                                                     currentTheme,
                                                                     isKickType
                                                                 }) => {
    if (isKickType) {
        return (
            <DetailCard title="Execution Time" accentColor={serverColor} currentTheme={currentTheme}>
                <DetailItem icon={<FaCalendarAlt />} label="Execution Date" value={executionDate} accentColor={serverColor} currentTheme={currentTheme} />
            </DetailCard>
        );
    }

    return (
        <DetailCard title="Time & Duration" accentColor={serverColor} currentTheme={currentTheme}>
            <DetailItem icon={<FaCalendarAlt />} label="Execution Date" value={executionDate} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaClock />} label="Duration" value={duration || null} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaCalendarAlt />} label="Expiration Date" value={expirationDate || null} accentColor={serverColor} currentTheme={currentTheme} />
        </DetailCard>
    );
};

export default TimeInformationCard;