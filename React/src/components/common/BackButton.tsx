import React from 'react';
import { FaArrowLeft } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import {useTranslation} from "react-i18next";

const BackButton: React.FC = ({  }) => {
    const navigate = useNavigate();
    const { t } = useTranslation();

    const handleGoBack = () => {
        if (window.history.length > 1) {
            navigate(-1);
        } else {
            navigate('/');
        }
    };

    return (
        <button
            onClick={handleGoBack}
            className={`
                        inline-flex items-center gap-2 px-4 py-2 rounded-full transition-all duration-200 
                        backdrop-blur-md border outline-none transform hover:scale-[1.02]
                        bg-surface border-surface-border text-text-primary hover:bg-surface-elevated
                      `}
            aria-label="Go back"
        >
            <FaArrowLeft className="text-sm opacity-80" />
            <span className="text-sm font-bold tracking-tight">{t("back-button.label")}</span>
        </button>
    );
};

export default BackButton;