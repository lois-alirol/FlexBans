import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
    FaTimes, FaGavel, FaBan,
    FaVolumeMute, FaHandPointRight, FaExclamationTriangle,
    FaUser, FaFingerprint, FaShieldAlt
} from 'react-icons/fa';
import { FaPenToSquare } from "react-icons/fa6";
import { Link } from 'react-router-dom';

import 'flag-icons/css/flag-icons.min.css';

import type { PunishmentType } from '@/types/punishments';

import { useAuth } from '@hooks/useAuth';

import MobileNavbar from '@components/common/sidebar/MobileNavbar';
import NavButton from '@components/common/sidebar/NavButton';
import SearchInput from '@components/common/sidebar/SearchInput';
import {useTranslation} from "react-i18next";
import {useServerConfig} from "@hooks/useServerConfig.ts";

interface SidebarProps {
    counts: Record<PunishmentType, number>;
    activeType: PunishmentType | null;
    setActiveType: (type: PunishmentType | null) => void;
    isSidebarOpen: boolean;
    setIsSidebarOpen: (isOpen: boolean) => void;
    onNewPunishmentClick: () => void;
    sidebarWidth: number;
    setSidebarWidth: (width: number) => void;
}

const MIN_WIDTH = 80;
const MAX_WIDTH = 450;
const COLLAPSE_THRESHOLD = 180;

const Sidebar: React.FC<SidebarProps> = ({
                                             counts, activeType, setActiveType,
                                             isSidebarOpen, setIsSidebarOpen, onNewPunishmentClick,
                                             sidebarWidth, setSidebarWidth
                                         }) => {
    const { isAuthenticated, user } = useAuth();
    const { serverConfig } = useServerConfig();
    const { t } = useTranslation();

    const isAdmin = user?.permissions?.includes('flexbans.web.admin');

    const [isResizing, setIsResizing] = useState(false);
    const sidebarRef = useRef<HTMLDivElement>(null);

    const startResizing = useCallback((e: React.MouseEvent) => {
        e.preventDefault();
        setIsResizing(true);
    }, []);

    const stopResizing = useCallback(() => {
        setIsResizing(false);
    }, []);

    const resize = useCallback((e: MouseEvent) => {
        if (isResizing) {
            let newWidth = e.clientX;
            if (newWidth < MIN_WIDTH) newWidth = MIN_WIDTH;
            if (newWidth > MAX_WIDTH) newWidth = MAX_WIDTH;
            setSidebarWidth(newWidth);
        }
    }, [isResizing, setSidebarWidth]);

    useEffect(() => {
        if (isResizing) {
            window.addEventListener('mousemove', resize);
            window.addEventListener('mouseup', stopResizing);
        } else {
            window.removeEventListener('mousemove', resize);
            window.removeEventListener('mouseup', stopResizing);
        }
        return () => {
            window.removeEventListener('mousemove', resize);
            window.removeEventListener('mouseup', stopResizing);
        };
    }, [isResizing, resize, stopResizing]);

    const isExpanded = sidebarWidth > COLLAPSE_THRESHOLD;

    const menuItems: { type: PunishmentType; icon: React.ReactNode; }[] = [
        { type: 'BAN', icon: <FaBan /> },
        { type: 'MUTE', icon: <FaVolumeMute /> },
        { type: 'KICK', icon: <FaHandPointRight /> },
        { type: 'WARNING', icon: <FaExclamationTriangle /> },
    ];

    return (
        <>
            <MobileNavbar
                onOpen={() => setIsSidebarOpen(true)}
            />

            <div
                className={`fixed inset-0 bg-background/40 backdrop-blur-sm z-150 lg:hidden transition-opacity duration-300 ease-in-out ${
                    isSidebarOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
                }`}
                onClick={() => setIsSidebarOpen(false)}
            />

            <aside
                ref={sidebarRef}
                className={`fixed inset-y-0 left-0 z-160 flex flex-col border-r
                            border-border-c text-modal-text-primary bg-sidebar-background
                            ${isResizing ? '' : 'transition-transform lg:transition-[width] duration-300'}
                            ease-in-out
                            ${isSidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
                `}
                style={{
                    width: typeof window !== 'undefined' && window.innerWidth < 1024 ? '280px' : `${sidebarWidth}px`,
                    backdropFilter: 'blur(12px)'
                }}
            >
                <div
                    onMouseDown={startResizing}
                    className="absolute top-0 -right-1 bottom-0 w-2 cursor-col-resize z-50 group hidden lg:block"
                >
                    <div
                        className={`bg-server-color h-full w-1 mx-auto transition duration-300 ease-in-out ${isResizing ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}
                    />
                </div>

                <div className="h-24 px-6 flex items-center justify-between shrink-0 overflow-hidden">
                    <Link to="/" className="flex items-center min-w-max">
                        <img
                            src={serverConfig ? serverConfig.serverLogo : 'http://example.com/'}
                            alt="Logo"
                            className="h-10 w-10 object-contain drop-shadow-md"
                        />

                        {(isExpanded || window.innerWidth < 1024) && (
                            <h1 className="ml-4 text-xl font-black">
                                {serverConfig ? serverConfig.serverName : ''}
                            </h1>
                        )}
                    </Link>
                    <button
                        onClick={() => setIsSidebarOpen(false)}
                        className="lg:hidden p-2 hover:bg-sidebar-surface rounded-full transition-colors text-sidebar-text-primary"
                    >
                        <FaTimes size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto px-4 py-2 custom-scrollbar overflow-x-hidden">
                    <nav className={`flex flex-col ${(isExpanded || window.innerWidth < 1024) ? 'items-stretch' : 'items-center'}`}>
                        {(isExpanded || window.innerWidth < 1024) && (
                            <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-sidebar-text-secondary mb-4 px-2">
                                {t("sidebar.navigation-title")}
                            </p>
                        )}

                        {menuItems.map(item => (
                            <NavButton
                                key={item.type}
                                type={item.type}
                                activeType={activeType}
                                count={counts[item.type]}
                                icon={item.icon}
                                onClick={() => {
                                    setActiveType(item.type);
                                    if(window.innerWidth < 1024) setIsSidebarOpen(false);
                                }}
                                isExpanded={isExpanded || window.innerWidth < 1024}
                                disabled={serverConfig ? !serverConfig.punishments[item.type.toLowerCase() + 's' as keyof typeof serverConfig.punishments]?.enabled : true}
                            />
                        ))}

                        {(isExpanded || window.innerWidth < 1024) && (
                            <div className="mt-8 pt-8 border-t border-border-c space-y-1">
                                <SearchInput label={t("sidebar.player-search.label")} placeholder={t("sidebar.player-search.placeholder")} icon={<FaUser size={12}/>} id="pS" route="player"/>
                                <SearchInput label={t("sidebar.moderator-search.label")} placeholder={t("sidebar.moderator-search.placeholder")} icon={<FaGavel size={12}/>} id="mS" route="moderator"/>
                                <SearchInput label={t("sidebar.punishment-search.label")} placeholder={t("sidebar.punishment-search.placeholder")} icon={<FaFingerprint size={12}/>} id="iS" route="punishment"/>
                            </div>
                        )}
                    </nav>
                </div>

                <div className={`p-4 mt-auto space-y-2 border-t border-border-c bg-sidebar-background`}>
                    {serverConfig && serverConfig.isSecured && isAuthenticated && isAdmin && (
                        <Link
                            to={'/admin'}
                            className="bg-sidebar-surface border border-border-c w-full flex items-center justify-center gap-3 py-3 rounded-full font-black text-[12px] uppercase tracking-widest text-sidebar-text-primary transition-all hover:-translate-y-px mb-2">
                            <FaShieldAlt size={16}/>
                            {(isExpanded || window.innerWidth < 1024) &&
                                <span>{t("sidebar.buttons.admin-panel")}</span>
                            }
                        </Link>
                    )}

                    {serverConfig && serverConfig.isSecured && isAuthenticated && (
                        <button
                            onClick={onNewPunishmentClick}
                            className="bg-sidebar-surface border border-border-c w-full flex items-center justify-center gap-3 py-3 rounded-full font-black text-[12px] uppercase tracking-widest text-sidebar-text-primary transition-all hover:-translate-y-px shadow-lg mb-2"
                        >
                            <FaPenToSquare size={16}/>
                            {(isExpanded || window.innerWidth < 1024) &&
                                <span>{t("sidebar.buttons.new-punishment")}</span>
                            }
                        </button>
                    )}

                    <div className="pt-2 text-center opacity-40 select-none">
                        <p className="text-[10px] font-medium uppercase tracking-[0.15em] text-sidebar-text-secondary whitespace-nowrap overflow-hidden">
                            {(isExpanded || window.innerWidth < 1024) ? (
                                `© ${new Date().getFullYear()} Neocle`
                            ) : (
                                `© ${new Date().getFullYear().toString().slice(-2)}`
                            )}
                        </p>
                    </div>
                </div>
            </aside>
        </>
    );
};

export default Sidebar;