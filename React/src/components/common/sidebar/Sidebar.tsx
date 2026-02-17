import React, { useState } from 'react';
import { FaSignOutAlt, FaTimes, FaPlus, FaGavel, FaBan, FaVolumeMute, FaHandPointRight, FaExclamationTriangle, FaUser, FaFingerprint, FaAngleRight } from 'react-icons/fa';
import { BsSunFill, BsMoonFill } from 'react-icons/bs';
import { Link } from 'react-router-dom';
import type {PunishmentType} from "../../../types/punishments.tsx";
import {useTheme} from "../../../hooks/useTheme.ts";
import type {ServerConfig} from "../../../types/config.tsx";
import {useAuth} from "../../../hooks/useAuth.ts";

interface SidebarProps {
    serverConfig: ServerConfig;
    counts: Record<PunishmentType, number>;
    activeType: PunishmentType | null;
    setActiveType: (type: PunishmentType | null) => void;
    isSidebarOpen: boolean;
    setIsSidebarOpen:  (isOpen: boolean) => void;
    onNewPunishmentClick: () => void;
}

const SearchInput: React.FC<{ label: string, placeholder: string, icon: React.ReactNode, id: string, route: string, currentTheme: 'dark' | 'light', isDesktopFullOpen: boolean }> = ({ label, placeholder, icon, id, route, currentTheme, isDesktopFullOpen }) => {
    const [searchValue, setSearchValue] = useState('');

    const searchPath = `/${route}/${encodeURIComponent(searchValue. trim())}`;
    const isDisabled = searchValue.trim() === '';

    return (
        <div className={`mb-4 transition-opacity duration-300 ${isDesktopFullOpen ? 'opacity-100' : 'opacity-0 hidden'}`}>
            <label htmlFor={id} className="block mb-2 text-sm font-semibold">{label}</label>
            <div className="relative">
                <input
                    type="text"
                    id={id}
                    placeholder={placeholder}
                    value={searchValue}
                    onChange={(e) => setSearchValue(e.target.value)}
                    className={`w-full p-2 pl-10 rounded-lg shadow-sm focus:outline-none text-sm ${currentTheme === 'dark' ?  'bg-[#2c2c2c] focus:ring-1' : 'bg-gray-100 focus:ring-1'}`}
                />
                <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
                    {icon}
                </div>
            </div>

            <Link
                to={searchPath}
                className={`cursor-pointer underline-offset-2 hover:underline`}
                onClick={(e) => {
                    if (isDisabled) {
                        e.preventDefault();
                    }
                }}
            >
                <button
                    disabled={isDisabled}
                    className={`mt-1 w-full py-1 text-xs rounded-lg font-semibold transition duration-300
            ${currentTheme === 'dark'
                        ? 'bg-[#3b3b3bcc] hover:bg-[#4b4b4bec] text-white'
                        : 'bg-gray-200 hover:text-gray-800'
                    }
            ${isDisabled ?  'cursor-not-allowed' : 'cursor-pointer'}
          `}
                >
                    Search
                </button>
            </Link>
        </div>
    );
};

const ToggleSidebarButton: React.FC<{ onClick: () => void, isClosing: boolean, currentTheme:  'dark' | 'light', className?:  string, style?: React.CSSProperties }> = ({ onClick, isClosing, currentTheme, className, style }) => {
    const baseClasses = `fixed top-1/2 h-10 w-10 flex items-center justify-center rounded-full shadow-lg`;
    const themeClasses = currentTheme === 'dark' ?  'bg-[#242424] text-white hover:bg-[#3b3b3bcc] border border-gray-700' : 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-300';

    return (
        <button
            onClick={onClick}
            aria-label={isClosing ? "Hide sidebar" : "Show sidebar"}
            className={`${baseClasses} ${themeClasses} ${className}`}
            style={{ transitionProperty: 'left, background-color, border-color', ...style }}
        >
            <FaAngleRight className={`text-xl ${isClosing ? 'rotate-180' : 'rotate-0'}`} />
        </button>
    );
};

const NavButton: React.FC<{
    type: PunishmentType;
    activeType: PunishmentType | null;
    count: number;
    icon: React.ReactNode;
    onClick: () => void;
    currentTheme: 'dark' | 'light';
    isDesktopFullOpen: boolean;
    disabled?: boolean;
    serverColor: string;
}> = ({ type, activeType, count, icon, onClick, currentTheme, isDesktopFullOpen, disabled, serverColor }) => {
    const isActive = type === activeType;

    const baseClasses =
        "flex py-3 rounded-xl transition-all duration-300 ease-in-out font-medium group shrink-0 items-center";

    const inactiveClasses = currentTheme === 'dark' ? "text-[#e0e0e0] hover:bg-[#3b3b3bcc]" : "text-[#333333] hover:bg-[#e6e6e6]";

    return (
        <button
            onClick={! disabled ? onClick : undefined}
            disabled={disabled}
            style={isActive ? {
                backgroundColor: serverColor,
                color: 'white',
                boxShadow: `0 4px 15px -5px ${serverColor}cc`,
            } : undefined}
            className={`${baseClasses}
              ${isDesktopFullOpen ? 'w-full' : 'w-14'}
              ${disabled ? "opacity-40 cursor-not-allowed ! hover:bg-transparent" : (isActive ? '' : inactiveClasses)}
              ${isDesktopFullOpen ? 'px-4 justify-between' : 'justify-center h-14'}
            `}
        >
            <div className={`flex items-center ${isDesktopFullOpen ? '' : 'justify-center'} w-full`}>
                <div className="shrink-0 w-6 flex justify-center">
                    {icon}
                </div>
                <span className={`text-base tracking-wide whitespace-nowrap transition-opacity duration-300 ${isDesktopFullOpen ? 'ml-4 opacity-100' : 'opacity-0 absolute left-full'}`}>{type}S</span>
                {isDesktopFullOpen && (
                    <span className={`ml-auto inline-flex items-center px-2 py-0.5 text-xs font-bold rounded-full transition-opacity duration-300 ${isActive ?  'bg-white' : (currentTheme === 'dark' ? 'bg-[#3b3b3bcc] text-gray-300' : 'bg-gray-200 text-gray-700')}`} style={isActive ? { color: serverColor } : {}}>
            {count}
          </span>
                )}
            </div>
        </button>
    );
};

const Sidebar: React.FC<SidebarProps> = ({ serverConfig, counts, activeType, setActiveType, isSidebarOpen, setIsSidebarOpen, onNewPunishmentClick }) => {
    const { isAuthenticated, logout } = useAuth();
    const { isDarkMode, setIsDarkMode, currentTheme } = useTheme();

    const punishmentEnabled:  Record<PunishmentType, boolean> = {
        BAN: serverConfig.punishments.bans.enabled,
        MUTE: serverConfig.punishments.mutes.enabled,
        KICK: serverConfig.punishments.kicks.enabled,
        WARNING: serverConfig.punishments.warnings.enabled,
    };

    const sideBarClasses = currentTheme === 'dark' ?  'bg-[#242424] text-[#e0e0e0]' : 'bg-white text-[#333333]';

    const isDesktopFullOpen = isSidebarOpen;

    const miniBorderClass = isDesktopFullOpen ? '' : (currentTheme === 'dark' ? 'lg:border-r lg:border-gray-700' :  'lg:border-r lg:border-gray-200');

    const menuItems:  { type: PunishmentType; icon: React.ReactNode; }[] = [
        { type:  'BAN', icon: <FaBan className="text-xl" /> },
        { type:  'MUTE', icon:  <FaVolumeMute className="text-xl" /> },
        { type: 'KICK', icon: <FaHandPointRight className="text-xl" /> },
        { type: 'WARNING', icon: <FaExclamationTriangle className="text-xl" /> },
    ];

    const darkFadeStyle:  React.CSSProperties = {
        boxShadow: `0 -10px 15px -3px rgba(36, 36, 36, 0.5), 0 -4px 6px -2px rgba(36, 36, 36, 0.05)`,
        borderTop: currentTheme === 'dark' ?  '1px solid #333333' : 'none',
        background: isDesktopFullOpen ? `linear-gradient(to top, #242424 50%, rgba(36, 36, 36, 0) 100%)` : 'none',
    };

    const lightFadeStyle: React.CSSProperties = {
        boxShadow: `0 -10px 15px -3px rgba(255, 255, 255, 0.5), 0 -4px 6px -2px rgba(0, 0, 0, 0.05)`,
        borderTop: currentTheme === 'light' ? '1px solid #e6e6e6' : 'none',
        background: isDesktopFullOpen ? `linear-gradient(to top, white 50%, rgba(255, 255, 255, 0) 100%)` : 'none',
    };


    return (
        <>
            <div
                className={`fixed inset-0 left-0 ${sideBarClasses} transform transition-[width,transform] duration-300 ease-in-out lg:w-20 ${isSidebarOpen ? 'lg:w-64 translate-x-0' : 'translate-x-0'} lg:flex lg:flex-col z-50 shadow-2xl ${miniBorderClass}`}
            >
                <ToggleSidebarButton
                    onClick={() => setIsSidebarOpen(false)}
                    isClosing={true}
                    currentTheme={currentTheme}
                    className={`hidden lg:flex transition-opacity duration-300 z-50 ${isDesktopFullOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}`}
                    style={{
                        right: 'auto',
                        left: isDesktopFullOpen ? 'calc(16rem - 20px)' : 'calc(5rem - 20px)',
                        transitionProperty: 'left, opacity'
                    }}
                />

                {isSidebarOpen && (
                    <button
                        onClick={() => setIsSidebarOpen(false)}
                        aria-label="Close menu"
                        className="lg:hidden text-2xl absolute top-4 right-4 text-gray-400 hover:text-white transition duration-300 z-50"
                    >
                        <FaTimes />
                    </button>
                )}

                <div className="flex flex-col h-full pt-4">

                    <div className={`flex items-center h-16 px-4 mb-4 shrink-0 transition-all duration-300 ${isDesktopFullOpen ? 'justify-start' : 'justify-center'}`}>
                        <img src={serverConfig.serverLogo} alt="Logo" className="h-10" />
                        <span className={`ml-3 text-xl font-extrabold whitespace-nowrap transition-opacity duration-300 ${isDesktopFullOpen ? 'opacity-100' : 'opacity-0 absolute left-full'}`}>{serverConfig.serverName}</span>
                    </div>

                    <nav className="grow space-y-2 overflow-y-auto overflow-x-hidden scrollbar-thin scrollbar-thumb-gray-600 scrollbar-track-transparent pb-4 px-4 flex flex-col items-center">
                        {menuItems.map(item => (
                            <NavButton
                                key={item. type}
                                type={item.type}
                                activeType={activeType}
                                count={counts[item.type]}
                                icon={item.icon}
                                onClick={() => {
                                    setActiveType(item.type);
                                    if (window.innerWidth < 1024) setIsSidebarOpen(false);
                                }}
                                currentTheme={currentTheme}
                                isDesktopFullOpen={isDesktopFullOpen}
                                disabled={!punishmentEnabled[item.type]}
                                serverColor={serverConfig.serverColor}
                            />
                        ))}

                        {isDesktopFullOpen && (
                            <div className={`flex flex-col p-4 mt-6 rounded-xl w-full ${currentTheme === 'dark' ?  'bg-[#1c1c1c] border border-[#333333]' : 'bg-gray-100 border border-gray-300'}`}>
                                <p className="text-xs uppercase font-bold tracking-wider mb-4 text-gray-400">Search Punishments</p>

                                <SearchInput label="Search Player" placeholder="Player Name/UUID" icon={<FaUser />} id="playerInput" route="player" currentTheme={currentTheme} isDesktopFullOpen={isDesktopFullOpen} />
                                <SearchInput label="Search Moderator" placeholder="Moderator Name/UUID" icon={<FaGavel />} id="moderatorInput" route="moderator" currentTheme={currentTheme} isDesktopFullOpen={isDesktopFullOpen} />
                                <SearchInput label="Search ID" placeholder="Punishment ID" icon={<FaFingerprint />} id="punishmentInput" route="punishment" currentTheme={currentTheme} isDesktopFullOpen={isDesktopFullOpen} />
                            </div>
                        )}

                        {isDesktopFullOpen && serverConfig.isSecured && isAuthenticated && serverConfig.punishmentExecution && (
                            <button
                                onClick={onNewPunishmentClick}
                                className={`group w-full flex items-center justify-between text-white font-semibold py-2.5 px-3.5 rounded-xl
                                            transition duration-200 ease-in-out shadow-sm hover:shadow-md hover:brightness-[0.98]
                                            active:translate-y-px
                                            ${currentTheme === 'dark' ? 'focus:ring-2 focus:ring-white/15' : 'focus:ring-2 focus:ring-black/8'}`}
                                style={{
                                    backgroundColor: serverConfig.serverColor,
                                    boxShadow: `0 6px 18px -10px ${serverConfig.serverColor}e6`,
                                    border: currentTheme === 'dark' ? '1px solid #ffffff1c' : '1px solid #ffffff44',
                                }}
                            >
                                <div className="flex items-center gap-2.5">
                                    <span
                                        className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/85 text-sm shadow-sm transition group-hover:bg-white"
                                        style={{ color: serverConfig.serverColor }}
                                    >
                                        <FaPlus />
                                    </span>
                                    <div className="flex flex-col text-left leading-tight">
                                        <span className="text-sm tracking-wide">New Punishment</span>
                                        <span className="text-[11px] font-normal opacity-80">Click to punish a player</span>
                                    </div>
                                </div>
                                <FaAngleRight className="text-base opacity-75 transition group-hover:opacity-100" />
                            </button>
                        )}
                    </nav>

                    <div
                        className="pb-4 pt-2 flex flex-col gap-2 shrink-0 px-3 relative z-10 transition-all duration-300"
                        style={currentTheme === 'dark' ?  darkFadeStyle : lightFadeStyle}
                    >
                        <button
                            onClick={() => setIsDarkMode(!isDarkMode)}
                            className={`flex items-center rounded-xl w-full text-left transition-colors duration-300 ease-in-out font-medium ${currentTheme === 'dark' ?  'hover:bg-[#3b3b3bcc]' : 'hover:bg-[#e6e6e6]'} ${isDesktopFullOpen ? 'justify-start py-2.5 px-4' : 'justify-center h-14'}`}
                        >
                            <div className="shrink-0 w-6 flex justify-center">
                                {isDarkMode ?
                                    <BsSunFill className="text-xl text-yellow-500" />
                                    :
                                    <BsMoonFill className="text-xl text-gray-600" />
                                }
                            </div>
                            <span className={`ml-4 text-base whitespace-nowrap transition-opacity duration-300 ${isDesktopFullOpen ? 'opacity-100' : 'opacity-0 absolute left-full'}`}>
                      {isDarkMode ? 'Light Mode' : 'Dark Mode'}
                  </span>
                        </button>

                        {serverConfig.isSecured && isAuthenticated &&
                            <button
                                onClick={logout}
                                className={`flex items-center rounded-xl w-full text-left transition-colors duration-300 ease-in-out font-medium ${currentTheme === 'dark' ? 'hover:bg-[#3b3b3bcc] text-red-400' : 'hover:bg-red-100 text-red-600'} ${isDesktopFullOpen ? 'justify-start py-2.5 px-4' : 'justify-center h-14'}`}
                            >
                                <div className="shrink-0 w-6 flex justify-center">
                                    <FaSignOutAlt className="text-xl" />
                                </div>
                                <span className={`ml-4 text-base whitespace-nowrap transition-opacity duration-300 ${isDesktopFullOpen ? 'opacity-100' : 'opacity-0 absolute left-full'}`}>Logout</span>
                            </button>
                        }
                    </div>
                </div>
            </div>

            <ToggleSidebarButton
                onClick={() => setIsSidebarOpen(! isSidebarOpen)}
                isClosing={isSidebarOpen}
                currentTheme={currentTheme}
                className={`lg:hidden z-40`}
                style={{ left: isSidebarOpen ? '236px' : '10px' }}
            />

            <ToggleSidebarButton
                onClick={() => setIsSidebarOpen(true)}
                isClosing={false}
                currentTheme={currentTheme}
                className={`hidden lg:flex transition-opacity duration-300 z-100 ${! isDesktopFullOpen ? 'opacity-100 pointer-events-auto' :  'opacity-0 pointer-events-none'}`}
                style={{
                    left: '60px',
                    transitionProperty: 'opacity'
                }}
            />
        </>
    );
};

export default Sidebar;