import React from 'react';

import BackButton from '@components/common/BackButton';
import {useTranslation} from "react-i18next";

interface HistoryPageHeaderProps {
    isConsole: boolean;
    isPlayerContext: boolean;
    bgColor: string;
    borderColor: string;
    contextBadgeStyle: string;
}

const HistoryPageHeader: React.FC<HistoryPageHeaderProps> = ({
                                                                 isConsole,
                                                                 isPlayerContext,
                                                                 bgColor,
                                                                 borderColor,
                                                                 contextBadgeStyle
                                                             }) => {
    const { t } = useTranslation();

    return (
        <div className={`sticky top-0 z-30 ${bgColor}/95 backdrop-blur-md border-b ${borderColor}`}>
            <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <BackButton />
                    <h2 className="text-lg font-bold">{t("history.title")}</h2>
                </div>
                <span className={`text-xs px-3 py-1 rounded-full font-bold uppercase tracking-wider ${contextBadgeStyle}`}>
                    {isConsole ? t("history.badges.console") : (isPlayerContext ? t("history.badges.player") : t("history.badges.staff"))}
                </span>
            </div>
        </div>
    );
};

export default HistoryPageHeader;