import React from 'react';
import { FaDownload } from 'react-icons/fa';
import PlayerSkin3D from '../common/PlayerSkin3D';
import PrefixBadge from '../common/PrefixBadge';

interface ProfileHeaderProps {
    username: string;
    prefix?: string;
    isPlayerContext: boolean;
    isDownloading: boolean;
    currentTheme: 'light' | 'dark';
    cardBg: string;
    borderColor: string;
    onDownloadSkin: () => void;
}

const ProfileHeader: React.FC<ProfileHeaderProps> = ({
                                                         username,
                                                         prefix,
                                                         isPlayerContext,
                                                         isDownloading,
                                                         currentTheme,
                                                         cardBg,
                                                         borderColor,
                                                         onDownloadSkin
                                                     }) => {
    return (
        <div className={`lg:col-span-4 rounded-2xl shadow-sm border ${borderColor} ${cardBg} p-8 flex flex-col items-center relative overflow-hidden group`}>
            <button
                onClick={onDownloadSkin}
                disabled={isDownloading}
                className={`absolute top-4 right-4 z-20 w-10 h-10 rounded-full flex items-center justify-center transition-all duration-200 shadow-lg border opacity-0 group-hover:opacity-100 ${
                    currentTheme === 'dark' ? 'bg-white/10 border-white/10 text-white hover:bg-white/20' : 'bg-black/5 border-black/5 text-black hover:bg-black/10'
                }`}
            >
                <FaDownload size={14} className={isDownloading ? 'animate-bounce' : ''} />
            </button>
            <div className="absolute top-0 w-full h-32 bg-linear-to-b from-blue-500/10 to-transparent"></div>
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
                <PlayerSkin3D username={username} width={220} height={340} theme={currentTheme} />
            </div>
        </div>
    );
};

export default ProfileHeader;