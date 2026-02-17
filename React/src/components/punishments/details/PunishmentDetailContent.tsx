import React, { useState } from 'react';
import PunishmentHeader from './PunishmentHeader';
import CoreInformationCard from "./cards/CoreInformationCard.tsx";
import TimeInformationCard from "./cards/TimeInformationCard.tsx";
import ServerContextCard from "./cards/ServerContextCard.tsx";
import ReasonSection from "./sections/ReasonSection.tsx";
import RemovalDetailsSection from "./sections/RemovalDetailsSection.tsx";
import RevokeButton from "./actions/RevokeButton.tsx";
import StatusMessage from "./actions/StatusMessage.tsx";
import {useServerConfig} from "../../../hooks/useServerConfig.ts";
import {useAuth} from "../../../hooks/useAuth.ts";
import type {PunishmentDetailData} from "../../../hooks/usePunishmentDetails.ts";
import BackButton from "../../common/BackButton.tsx";
import RevokeModal from "../../common/modals/RevokeModal.tsx";

type Theme = 'dark' | 'light';

interface PunishmentDetailContentProps extends PunishmentDetailData {
    server_color: string;
    server_color_hover: string;
    currentTheme: Theme;
}

const PunishmentDetailContent: React.FC<PunishmentDetailContentProps> = ({
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
                                                                             server_color,
                                                                             server_color_hover,
                                                                             currentTheme,
                                                                         }) => {
    const { serverConfig } = useServerConfig();
    const { user } = useAuth();

    const [isModalOpen, setIsModalOpen] = useState(false);

    const isKickType = punishment_type?.toLowerCase() === 'kick' || punishment_type?.toLowerCase() === 'kicks';

    const textColor = currentTheme === 'dark' ? 'text-gray-100' : 'text-gray-800';
    const bodyBg = currentTheme === 'dark' ? 'bg-[#1c1c1c]' : 'bg-gray-50';
    const cardBg = currentTheme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white';
    const shadow = 'shadow-lg';

    const isRemoved = status === 'Removed';
    const isExpired = status === 'Expired';
    const isActive = status === 'Active';

    return (
        <div className={`min-h-screen ${bodyBg} ${textColor} font-sans`}>
            <div className="container mx-auto p-4 sm:p-8 max-w-5xl">

                <div className="mb-6">
                    <BackButton currentTheme={currentTheme} />
                </div>

                <div className={`p-6 sm:p-8 rounded-2xl ${cardBg} ${shadow}`}>

                    <PunishmentHeader
                        punishmentType={punishment_type}
                        player={player}
                        punishmentId={punishment_id}
                        status={status}
                        serverColor={server_color}
                        currentTheme={currentTheme}
                        isKickType={isKickType}
                    />

                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                        <CoreInformationCard
                            player={player}
                            executor={executor}
                            punishmentType={punishment_type}
                            databaseId={database_id}
                            serverColor={server_color}
                            currentTheme={currentTheme}
                        />

                        <TimeInformationCard
                            executionDate={execution_date}
                            duration={duration}
                            expirationDate={expiration_date}
                            serverColor={server_color}
                            currentTheme={currentTheme}
                            isKickType={isKickType}
                        />

                        <ServerContextCard
                            originServer={origin_server}
                            scopeServer={scope_server}
                            ipScope={ip_scope}
                            serverColor={server_color}
                            currentTheme={currentTheme}
                        />
                    </div>

                    <ReasonSection
                        reason={reason}
                        serverColor={server_color}
                        currentTheme={currentTheme}
                        cardBg={cardBg}
                    />

                    {isRemoved && (
                        <RemovalDetailsSection
                            removerName={remover_name}
                            removalReason={removal_reason}
                        />
                    )}

                    <div className='mt-8'>
                        {serverConfig.isSecured && user && isActive && !isKickType && serverConfig.punishmentRevocation && (
                            <RevokeButton
                                onClick={() => setIsModalOpen(true)}
                                isRevoking={false}
                                serverColor={server_color}
                                serverColorHover={server_color_hover}
                            />
                        )}
                        {(serverConfig.isSecured && (isRemoved || isExpired)) && !isKickType && (
                            <StatusMessage isRemoved={isRemoved} />
                        )}
                    </div>
                </div>
            </div>

            {user &&
                <RevokeModal
                    isOpen={isModalOpen}
                    onClose={() => setIsModalOpen(false)}
                    removerName={user.username}
                    currentTheme={currentTheme}
                    punishmentId={database_id}
                    punishmentType={punishment_type}
                />
            }
        </div>
    );
};

export default PunishmentDetailContent;