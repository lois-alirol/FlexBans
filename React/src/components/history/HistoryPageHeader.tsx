import React from 'react';
import BackButton from '../common/BackButton';

interface HistoryPageHeaderProps {
    isConsole: boolean;
    isPlayerContext: boolean;
    currentTheme: 'light' | 'dark';
    bgColor: string;
    borderColor: string;
    contextBadgeStyle: string;
}

const HistoryPageHeader: React.FC<HistoryPageHeaderProps> = ({
                                                                 isConsole,
                                                                 isPlayerContext,
                                                                 currentTheme,
                                                                 bgColor,
                                                                 borderColor,
                                                                 contextBadgeStyle
                                                             }) => {
    return (
        <div className={`sticky top-0 z-30 ${bgColor}/95 backdrop-blur-md border-b ${borderColor}`}>
            <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <BackButton currentTheme={currentTheme} />
                    <h2 className="text-lg font-bold">History Overview</h2>
                </div>
                <span className={`text-xs px-3 py-1 rounded-full font-bold uppercase tracking-wider ${contextBadgeStyle}`}>
                    {isConsole ? "System Profile" : (isPlayerContext ? "Player Profile" : "Staff Profile")}
                </span>
            </div>
        </div>
    );
};

export default HistoryPageHeader;