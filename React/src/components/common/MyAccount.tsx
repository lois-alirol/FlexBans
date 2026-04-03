import React, { useState, useRef, useEffect } from 'react';
import { FaUserCircle, FaSignOutAlt, FaCog, FaUser } from 'react-icons/fa';

import { useAuth } from '@hooks/useAuth';

import PreferencesModal from '@components/common/modals/PreferencesModal';
import {useTranslation} from "react-i18next";

const MyAccount: React.FC<{ onPreferencesApply: () => void }> = ({ onPreferencesApply }) => {
    const { user, isAuthenticated, logout } = useAuth();
    const { t } = useTranslation();

    const accountName = isAuthenticated && user ? user.username : '?';
    const isAdmin = user?.permissions?.includes('flexbans.web.admin');

    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);

    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const menuClasses = 'bg-modal-background text-modal-text-primary border-border-c';
    const itemHoverClass = 'hover:bg-modal-surface-hover hover:text-modal-text-primary';

    const handleOpenSettings = () => {
        setIsDropdownOpen(false);
        setIsSettingsOpen(true);
    };

    return (
        <div className="flex items-center gap-2 relative">
            <div className="relative" ref={dropdownRef}>
                <button
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    className={`
                        flex items-center gap-2 px-4 py-2 rounded-full transition-all duration-200 
                        backdrop-blur-md border outline-none
                        bg-surface border-surface-border text-text-primary hover:bg-surface-elevated
                    `}
                >
                    <FaUserCircle className="text-lg" />
                    <span className="text-sm font-bold tracking-tight">{accountName}</span>
                </button>

                {isDropdownOpen && (
                    <div
                        className={`
                            absolute right-0 mt-3 w-60 rounded-2xl shadow-[0_15px_45px_rgba(0,0,0,0.4)]
                            backdrop-blur-xl border overflow-hidden z-100
                            animate-in fade-in zoom-in-95 duration-200
                            ${menuClasses}
                        `}
                    >
                        <div className="px-5 py-4 border-b border-border-c">
                            <p className="text-xs font-semibold uppercase tracking-widest text-modal-text-secondary mb-1">
                                {t("account-menu.title")}
                            </p>
                            <p className="text-base font-bold truncate">{accountName}</p>
                            <span
                                className="bg-server-color/15 text-server-color border-server-color/30 inline-block mt-2 px-2 py-0.5 text-[10px] font-bold uppercase rounded-md tracking-tighter"
                            >
                                {isAdmin ? t("account-menu.badges.admin") : t("account-menu.badges.user")}
                            </span>
                        </div>

                        <div className="p-2">
                            <MenuButton
                                icon={<FaUser />}
                                label={t("account-menu.buttons.profile")}
                                hoverClass={itemHoverClass}
                                onClick={() => {}}
                            />
                            <MenuButton
                                icon={<FaCog />}
                                label={t("account-menu.buttons.settings")}
                                hoverClass={itemHoverClass}
                                onClick={handleOpenSettings}
                            />
                        </div>

                        <div className="p-2 border-t border-border-c">
                            <button
                                className="
                                    w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold
                                    text-logout hover:bg-logout-hover transition-all duration-200
                                "
                                onClick={() => logout()}
                            >
                                <FaSignOutAlt className="opacity-80" />
                                {t("account-menu.buttons.log-out")}
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <PreferencesModal
                isOpen={isSettingsOpen}
                onClose={() => setIsSettingsOpen(false)}
                onApply={onPreferencesApply}
            />
        </div>
    );
};

interface MenuButtonProps {
    icon: React.ReactNode;
    label: string;
    onClick: () => void;
    hoverClass: string;
    color?: string;
}

const MenuButton = ({ icon, label, onClick, hoverClass, color }: MenuButtonProps) => (
    <button
        className={`
            w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium
            transition-all duration-200 group ${hoverClass}
        `}
        onClick={onClick}
    >
        <span className="opacity-60 group-hover:opacity-100 transition-opacity" style={{ color }}>
            {icon}
        </span>
        {label}
    </button>
);

export default MyAccount;