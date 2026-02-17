import React from 'react';
import { FaFingerprint } from 'react-icons/fa';
import StatusBadge from "../../common/StatusBadge.tsx";

type Theme = 'dark' | 'light';

interface PunishmentHeaderProps {
    punishmentType: string;
    player: string;
    punishmentId: string;
    status: string | null;
    serverColor: string;
    currentTheme: Theme;
    isKickType: boolean;
}

const PunishmentHeader: React.FC<PunishmentHeaderProps> = ({
                                                               punishmentType,
                                                               player,
                                                               punishmentId,
                                                               status,
                                                               serverColor,
                                                               currentTheme,
                                                               isKickType
                                                           }) => {
    return (
        <>
            <h1 className={`text-center text-3xl font-extrabold mb-4 ${currentTheme === 'dark' ? 'text-white' : 'text-gray-800'}`}>
                {punishmentType} on {player}
            </h1>

            <div className="flex flex-wrap justify-center items-center space-x-4 mb-8">
                {!isKickType && (
                    <StatusBadge
                        status={status}
                        className="text-base font-bold px-4 py-2"
                    />
                )}
                <div
                    className={`text-base font-bold py-2 px-4 inline-flex items-center rounded-full border ${currentTheme === 'dark' ? 'border-gray-600/30' : 'border-gray-300'}`}
                    style={{
                        color: serverColor,
                        backgroundColor: currentTheme === 'dark' ? 'rgba(79, 70, 229, 0.1)' : 'rgba(79, 70, 229, 0.05)',
                    }}
                >
                    <FaFingerprint className="mr-2 text-base" />
                    <span className="tracking-wider">ID: {punishmentId}</span>
                </div>
            </div>
        </>
    );
};

export default PunishmentHeader;