import React from 'react';
import { FaUser, FaUserShield, FaListAlt, FaFingerprint } from 'react-icons/fa';
import DetailCard from './DetailCard';
import DetailItem from './DetailItem';

type Theme = 'dark' | 'light';

interface CoreInformationCardProps {
    player: string;
    executor: string;
    punishmentType: string;
    databaseId: number;
    serverColor: string;
    currentTheme: Theme;
}

const CoreInformationCard: React.FC<CoreInformationCardProps> = ({
                                                                     player,
                                                                     executor,
                                                                     punishmentType,
                                                                     databaseId,
                                                                     serverColor,
                                                                     currentTheme
                                                                 }) => {
    return (
        <DetailCard title="Core Information" accentColor={serverColor} currentTheme={currentTheme}>
            <DetailItem icon={<FaUser />} label="Player" value={player} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaUserShield />} label="Executor" value={executor} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaListAlt />} label="Type" value={punishmentType} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaFingerprint />} label="Database ID" value={databaseId.toString()} accentColor={serverColor} currentTheme={currentTheme} />
        </DetailCard>
    );
};

export default CoreInformationCard;