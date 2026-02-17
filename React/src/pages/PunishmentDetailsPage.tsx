import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {useServerConfig} from "../hooks/useServerConfig.ts";
import {useTheme} from "../hooks/useTheme.ts";
import {usePunishmentDetails} from "../hooks/usePunishmentDetails.ts";
import PunishmentDetailContent from "../components/punishments/details/PunishmentDetailContent.tsx";
import LoadingSpinner from "../components/punishments/details/LoadingSpinner.tsx";

const PunishmentDetailPage: React.FC = () => {
    const { serverConfig } = useServerConfig();
    const { currentTheme } = useTheme();

    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const { punishmentDetails, isLoading, error } = usePunishmentDetails(id);

    useEffect(() => {
        if (!isLoading && (!id || !punishmentDetails || error)) {
            navigate("/unknown");
        }
    }, [id, punishmentDetails, isLoading, error, navigate]);

    if (isLoading) {
        return <LoadingSpinner currentTheme={currentTheme} serverColor={serverConfig.serverColor} />;
    }

    if (!punishmentDetails) {
        return null;
    }

    return (
        <PunishmentDetailContent
            {...punishmentDetails}
            server_color={serverConfig.serverColor}
            server_color_hover={serverConfig.serverColorHover}
            currentTheme={currentTheme}
        />
    );
};

export default PunishmentDetailPage;