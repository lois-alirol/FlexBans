import React from 'react';
import { FaFingerprint } from 'react-icons/fa';

import StatusBadge from '@components/common/StatusBadge';
import {useTranslation} from "react-i18next";

interface PunishmentHeaderProps {
    punishmentType: string;
    player: string;
    punishmentId: string;
    status: string | null;
    isKickType: boolean;
}

const PunishmentHeader: React.FC<PunishmentHeaderProps> = ({
                                                               punishmentType,
                                                               player,
                                                               punishmentId,
                                                               status,
                                                               isKickType
                                                           }) =>

{
    const { t } = useTranslation();

    return (
        <>
            <h1 className={`text-center text-3xl font-extrabold mb-4 text-text-primary`}>
                {t("details.title",
                    {
                        type: punishmentType[0].toUpperCase() + punishmentType.slice(1).toLowerCase(),
                        player: player
                    }
                )}
            </h1>

            <div className="flex flex-wrap justify-center items-center space-x-4 mb-8">
                {!isKickType && (
                    <StatusBadge
                        status={status}
                        className="text-base font-bold px-4 py-2"
                    />
                )}
                <div
                    className={`text-base font-bold py-2 px-4 inline-flex items-center rounded-full border border-surface-border bg-surface-elevated text-server-color`}
                >
                    <FaFingerprint className="mr-2 text-base" />
                    <span className="tracking-wider">ID: {punishmentId}</span>
                </div>
            </div>
        </>
    );
};

export default PunishmentHeader;