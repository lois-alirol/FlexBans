import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import {usePunishmentDetails} from '@hooks/usePunishmentDetails';
import {useTitle} from "@hooks/useTitle.ts";

import PunishmentDetailContent from '@components/punishments/details/PunishmentDetailContent';
import LoadingSpinner from '@components/punishments/details/LoadingSpinner';

const PunishmentDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const { punishmentDetails, isLoading, error } = usePunishmentDetails(id);

    useTitle(
        `${(id ? `#${id}` : "?")}`
    );

    useEffect(() => {
        if (!isLoading && (!id || !punishmentDetails || error)) {
            navigate(`/${id}`, { replace: true });
        }
    }, [id, punishmentDetails, isLoading, error, navigate]);

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!punishmentDetails) {
        return null;
    }

    return (
        <PunishmentDetailContent
            {...punishmentDetails}
        />
    );
};

export default PunishmentDetailPage;