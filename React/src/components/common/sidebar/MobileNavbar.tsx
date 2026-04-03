import React from 'react';
import {FaBars} from 'react-icons/fa';
import {useServerConfig} from "@hooks/useServerConfig.ts";

const MobileNavbar: React.FC<{
    onOpen: () => void;
}> = ({ onOpen }) => {
    const { serverConfig} = useServerConfig();

    return (
        <div className={`lg:hidden fixed top-0 left-0 right-0 h-16 flex items-center justify-between 
                         px-4 z-140 border-b backdrop-blur-md bg-sidebar-background border-border-c`}>
            <div className="flex items-center gap-3">
                <img src={serverConfig ? serverConfig.serverLogo : 'http://example.com/'} alt="Logo" className="h-8 w-8 object-contain" />
                <span className={`font-black uppercase tracking-tighter text-sm text-sidebar-text-primary`}>
                    {serverConfig ? serverConfig.serverName : 'http://example.com/'}
                </span>
            </div>
            <button
                onClick={onOpen}
                className={`p-2 rounded-lg text-sidebar-text-primary hover:bg-border-c`}
            >
                <FaBars size={20} />
            </button>
        </div>
    );
};

export default MobileNavbar;