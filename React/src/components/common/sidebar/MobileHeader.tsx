import React from 'react';
import { FaBars } from 'react-icons/fa';

interface MobileHeaderProps {
    onMenuClick: () => void;
    serverLogo: string;
    currentTheme: 'dark' | 'light';
}

const MobileHeader: React.FC<MobileHeaderProps> = ({ onMenuClick, serverLogo, currentTheme }) => {
    return (
        <header className={`flex items-center justify-between p-4 ${currentTheme === 'dark' ? 'bg-[#242424]' : 'bg-[#e4e4e4]'} lg:hidden shadow-md`}>
            <button onClick={onMenuClick} aria-label="Open menu" className="text-xl">
                <FaBars />
            </button>
            <img src={serverLogo} alt="Logo" className="h-6" />
        </header>
    );
};

export default MobileHeader;