import React, { useState } from 'react';

import type {PunishmentDetailData} from '@/types/punishments';

import {useServerConfig} from '@hooks/useServerConfig';
import {useAuth} from '@hooks/useAuth';

import PunishmentHeader from '@components/punishments/details/PunishmentHeader';

import CoreInformationCard from '@components/punishments/details/cards/CoreInformationCard';
import TimeInformationCard from '@components/punishments/details/cards/TimeInformationCard';
import ServerContextCard from '@components/punishments/details/cards/ServerContextCard';

import ReasonSection from '@components/punishments/details/sections/ReasonSection';
import RemovalDetailsSection from '@components/punishments/details/sections/RemovalDetailsSection';

import RevokeButton from '@components/punishments/details/actions/RevokeButton';

import BackButton from '@components/common/BackButton';
import RevokeModal from '@components//common/modals/RevokeModal';
import Notification from "@components/common/Notification.tsx";
import {useTranslation} from "react-i18next";

const PunishmentDetailContent: React.FC<PunishmentDetailData> = ({
                                                                             database_id,
                                                                             punishment_id,
                                                                             punishment_type,
                                                                             player,
                                                                             executor,
                                                                             reason,
                                                                             execution_date,
                                                                             expiration_date,
                                                                             duration,
                                                                             origin_server,
                                                                             scope_server,
                                                                             ip_scope,
                                                                             remover_name,
                                                                             removal_reason,
                                                                             status,
                                                                         }) => {
    const { serverConfig } = useServerConfig();
    const { user } = useAuth();
    const { t } = useTranslation();

    const [isModalOpen, setIsModalOpen] = useState(false);
    const isKickType = punishment_type?.toLowerCase() === 'kick' ||
                                punishment_type?.toLowerCase() === 'kicks';

    const [notification, setNotification] = useState<{
        visible: boolean;
        message: string;
        type: 'success' | 'error' | 'info';
    }>({
        visible: false,
        message: '',
        type: 'info',
    });

    const triggerNotif = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
        setNotification({ visible: true, message, type });
    };

    const handleRevokeSuccess = () => {
        setIsModalOpen(false);
        triggerNotif(t("revoke-modal.success.message"), 'success');
    };

    const containerClasses = 'bg-surface border-surface-border';

    const isRemoved = status === 'Removed';
    const isActive = status === 'Active';

    return (
        <div className={`min-h-screen bg-background text-text-primary`}>
            <Notification
                message={notification.message}
                type={notification.type}
                visible={notification.visible}
                onClose={() => setNotification(prev => ({ ...prev, visible: false }))}
            />

            <div className="container mx-auto p-4 sm:p-8 max-w-5xl">

                <div className="mb-6">
                    <BackButton />
                </div>

                <div className={`p-6 sm:p-8 rounded-2xl border ${containerClasses}`}>

                    <PunishmentHeader
                        punishmentType={punishment_type}
                        player={player}
                        punishmentId={punishment_id}
                        status={status}
                        isKickType={isKickType}
                    />

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                        <CoreInformationCard
                            player={player}
                            executor={executor}
                            punishmentType={punishment_type}
                            databaseId={database_id}
                        />

                        <TimeInformationCard
                            executionDate={execution_date}
                            duration={duration}
                            expirationDate={expiration_date}
                            isKickType={isKickType}
                        />

                        <ServerContextCard
                            originServer={origin_server}
                            scopeServer={scope_server}
                            ipScope={ip_scope}
                        />
                    </div>

                    <ReasonSection
                        reason={reason}
                    />

                    {isRemoved && (
                        <RemovalDetailsSection
                            removerName={remover_name}
                            removalReason={removal_reason}
                        />
                    )}

                    <div className='mt-8'>
                        {serverConfig && serverConfig.isSecured && user && isActive && !isKickType && serverConfig.punishmentRevocation && (
                            <RevokeButton
                                onClick={() => setIsModalOpen(true)}
                                isRevoking={false}
                            />
                        )}
                    </div>
                </div>
            </div>

            {user &&
                <RevokeModal
                    isOpen={isModalOpen}
                    onClose={() => setIsModalOpen(false)}
                    onSuccess={handleRevokeSuccess}
                    removerName={user.username}
                    punishmentId={database_id}
                    punishmentType={punishment_type}
                />
            }
        </div>
    );
};

export default PunishmentDetailContent;