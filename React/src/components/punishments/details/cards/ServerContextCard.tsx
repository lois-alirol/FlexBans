import React from 'react';
import { FaServer, FaNetworkWired } from 'react-icons/fa';
import DetailCard from './DetailCard';
import DetailItem from './DetailItem';

type Theme = 'dark' | 'light';

interface ServerContextCardProps {
    originServer: string;
    scopeServer: string;
    ipScope: boolean;
    serverColor: string;
    currentTheme: Theme;
}

const ServerContextCard: React.FC<ServerContextCardProps> = ({
                                                                 originServer,
                                                                 scopeServer,
                                                                 ipScope,
                                                                 serverColor,
                                                                 currentTheme
                                                             }) => {
    return (
        <DetailCard title="Server & Context" accentColor={serverColor} currentTheme={currentTheme}>
            <DetailItem icon={<FaServer />} label="Origin Server" value={originServer} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem icon={<FaServer />} label="Scope Server" value={scopeServer} accentColor={serverColor} currentTheme={currentTheme} />
            <DetailItem
                icon={<FaNetworkWired />}
                label="IP Scope"
                value={ipScope ? "Yes" : "No"}
                accentColor={serverColor}
                currentTheme={currentTheme}
            />
        </DetailCard>
    );
};

export default ServerContextCard;