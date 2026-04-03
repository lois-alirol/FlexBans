import React from 'react';
import { FaTimesCircle } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

interface RemovalDetailsSectionProps {
    removerName: string | null | undefined;
    removalReason: string | null | undefined;
}

const RemovalDetailsSection: React.FC<RemovalDetailsSectionProps> = ({ removerName, removalReason }) => {
    const { t } = useTranslation();

    return (
        <div className="mb-8 p-6 rounded-xl bg-red-500/15 border border-red-500/40 shadow-md">
            <h2 className="text-lg font-bold mb-3 text-red-400 border-b border-red-500/40 pb-2 flex items-center space-x-2">
                <FaTimesCircle className="text-xl"/> <span>{t("details.removal.title")}</span>
            </h2>
            <p className="text-red-300 text-base mb-2"><strong>{t("details.removal.remover")}</strong> {removerName}</p>
            <p className="text-red-300 text-base"><strong>{t("details.removal.reason")}</strong> {removalReason}</p>
        </div>
    );
};

export default RemovalDetailsSection;