import React from 'react';
import { FaCalendarAlt, FaClock } from 'react-icons/fa';

import DetailCard from '@/components/punishments/details/cards/DetailCard';
import DetailItem from '@/components/punishments/details/cards/DetailItem';
import {useTranslation} from "react-i18next";

interface TimeInformationCardProps {
    executionDate: string;
    duration?: string | null;
    expirationDate?: string | null;
    isKickType: boolean;
}

const TimeInformationCard: React.FC<TimeInformationCardProps> = ({
                                                                     executionDate,
                                                                     duration,
                                                                     expirationDate,
                                                                     isKickType
                                                                 }) =>
{
    const { t } = useTranslation();

    if (isKickType) {
        return (
            <DetailCard title={t("details.time.title")}>
                <DetailItem icon={<FaCalendarAlt />} label={t("details.time.items-titles.execution")} value={executionDate} />
            </DetailCard>
        );
    }

    return (
        <DetailCard title={t("details.time.title")} >
            <DetailItem icon={<FaCalendarAlt />} label={t("details.time.items-titles.execution")} value={executionDate} />
            <DetailItem icon={<FaClock />} label={t("details.time.items-titles.duration")} value={duration || null} />
            <DetailItem icon={<FaCalendarAlt />} label={t("details.time.items-titles.expiration")} value={expirationDate || null} />
        </DetailCard>
    );
};

export default TimeInformationCard;