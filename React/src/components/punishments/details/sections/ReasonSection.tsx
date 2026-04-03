import React from 'react';
import { FaInfoCircle } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

interface ReasonSectionProps {
    reason: string;
}

const ReasonSection: React.FC<ReasonSectionProps> = ({ reason }) => {
    const { t } = useTranslation();

    return (
        <div className={`mb-8 p-6 rounded-2xl bg-surface-elevated border border-surface-border`}>
            <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center border-surface-border text-server-color`}>
                <FaInfoCircle className='mr-2' /> {t("details.reason")}
            </h2>
            <p className="whitespace-pre-wrap text-text-primary">{reason}</p>
        </div>
    );
};

export default ReasonSection;