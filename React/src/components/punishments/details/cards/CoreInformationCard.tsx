import React from 'react';
import { FaUser, FaUserShield, FaListAlt, FaFingerprint } from 'react-icons/fa';

import DetailCard from '@/components/punishments/details/cards/DetailCard';
import DetailItem from '@/components/punishments/details/cards/DetailItem';
import {useTranslation} from "react-i18next";

interface CoreInformationCardProps {
    player: string;
    executor: string;
    punishmentType: string;
    databaseId: number;
}

const CoreInformationCard: React.FC<CoreInformationCardProps> = ({
                                                                     player,
                                                                     executor,
                                                                     punishmentType,
                                                                     databaseId,
                                                                 }) =>

{
    const { t } = useTranslation();

    return (
        <DetailCard title={t("details.core.title")}>
            <DetailItem icon={<FaUser />} label={t("details.core.items-titles.player")} value={player} />
            <DetailItem icon={<FaUserShield />} label={t("details.core.items-titles.executor")} value={executor} />
            <DetailItem icon={<FaListAlt />} label={t("details.core.items-titles.type")} value={punishmentType} />
            <DetailItem icon={<FaFingerprint />} label={t("details.core.items-titles.database-id")} value={databaseId.toString()} />
        </DetailCard>
    );
};

export default CoreInformationCard;