import React from 'react';
import { FaDownload } from 'react-icons/fa';

import PlayerSkin3D from '@components/common/PlayerSkin3D';
import PrefixBadge from '@components/common/PrefixBadge';

interface ProfileHeaderProps {
    username: string;
    prefix?: string;
    isPlayerContext: boolean;
    isDownloading: boolean;
    cardBg: string;
    onDownloadSkin: () => void;
}

const ProfileHeader: React.FC<ProfileHeaderProps> = ({
                                                         username,
                                                         prefix,
                                                         isPlayerContext,
                                                         isDownloading,
                                                         cardBg,
                                                         onDownloadSkin
                                                     }) => {
    return (
        <div className={`lg:col-span-4 rounded-2xl shadow-sm border border-surface-border ${cardBg} p-8 mb-4 flex flex-col items-center relative overflow-hidden group`}>
            <button
                onClick={onDownloadSkin}
                disabled={isDownloading}
                className={`absolute top-4 right-4 z-20 w-10 h-10 rounded-full flex items-center 
                            justify-center transition-all duration-200 shadow-lg border opacity-0 
                            group-hover:opacity-100 bg-surface border-surface-border text-text-primary 
                            hover:bg-surface-elevated
                          `}
            >
                <FaDownload size={14} className={isDownloading ? 'animate-bounce' : ''} />
            </button>
            <div className="absolute top-0 w-full h-32 bg-linear-to-b"></div>
            <div className="z-10 text-center mb-4">
                <h1 className="text-3xl font-black z-10 break-all leading-tight">{username}</h1>
                <PrefixBadge
                    prefix={prefix}
                    username={username}
                    isPlayerContext={isPlayerContext}
                    size="md"
                    className="mt-2"
                />
            </div>
            <div className="relative z-10 hover:scale-105 transition-transform duration-500">
                <PlayerSkin3D username={username} width={220} height={340} />
            </div>
        </div>
    );
};

export default ProfileHeader;