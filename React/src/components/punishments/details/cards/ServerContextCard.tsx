import React from 'react';
import { FaServer, FaNetworkWired } from 'react-icons/fa';

import DetailCard from '@/components/punishments/details/cards/DetailCard';
import DetailItem from '@/components/punishments/details/cards/DetailItem';
import {useTranslation} from "react-i18next";

interface ServerContextCardProps {
    originServer: string;
    scopeServer: string;
    ipScope: boolean;
}

const ServerContextCard: React.FC<ServerContextCardProps> = ({
                                                                 originServer,
                                                                 scopeServer,
                                                                 ipScope,
                                                             }) =>
{
    const { t } = useTranslation();

    return (
        <DetailCard title={t("details.server.title")} >
            <DetailItem icon={<FaServer />} label={t("details.server.items-titles.origin")} value={originServer} />
            <DetailItem icon={<FaServer />} label={t("details.server.items-titles.scope")} value={scopeServer} />
            <DetailItem
                icon={<FaNetworkWired />}
                label={t("details.server.items-titles.ip")}
                value={ipScope ? t("yes") : t("no")}
            />
        </DetailCard>
    );
};

export default ServerContextCard;