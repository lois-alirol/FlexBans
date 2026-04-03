import React, { useRef, useEffect } from 'react';
import { FaEye, FaTrash, FaEdit, FaSpinner } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

import type { Punishment } from '@/types/punishments';

import { useServerConfig } from '@hooks/useServerConfig';
import {useTranslation} from "react-i18next";

interface PunishmentContextMenuProps {
    position: { x: number; y: number };
    side: 'top' | 'bottom';
    punishment: Punishment;
    onClose: () => void;
    onRevoke: () => void;
    onEdit: () => void;
    isRevoking: boolean;
}

const PunishmentContextMenu: React.FC<PunishmentContextMenuProps> = ({
                                                                         position,
                                                                         side,
                                                                         punishment,
                                                                         onClose,
                                                                         onRevoke,
                                                                         onEdit,
                                                                         isRevoking,
                                                                     }) => {
    const menuRef = useRef<HTMLDivElement>(null);

    const navigate = useNavigate();
    const { serverConfig } = useServerConfig();
    const { t } = useTranslation();

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                onClose();
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    const handleAction = (action: () => void) => {
        action();
        onClose();
    };

    const menuClasses = 'bg-modal-background text-modal-text-primary border-border-c';
    const hoverClass = 'hover:bg-modal-surface-hover';

    const isKick = punishment.type.toLowerCase().includes('kick');
    const transformStyle = side === 'top' ? { transform: 'translateY(-18px)' } : { transform: 'translateY(calc(-100% + 18px))' };
    const arrowPlacement = side === 'top' ? 'top-3' : 'bottom-3';

    return (
        <div
            ref={menuRef}
            className={`absolute z-50 rounded-2xl border backdrop-blur-xl w-52 py-2 
                        animate-in fade-in zoom-in-95 duration-200 ${menuClasses}`}
            style={{
                top: position.y,
                left: position.x + 15,
                ...transformStyle,
            }}
        >
            <div
                className={`
                    absolute -left-1.5 ${arrowPlacement} w-3 h-3 border-l border-b 
                    transform rotate-45 z-0 bg-modal-background border-border-c
                `}
            />

            <div className="flex flex-col relative z-10">
                {serverConfig && serverConfig.detailsPageEnabled && (
                    <button
                        onClick={() => handleAction(() => navigate(`/punishment/${punishment.punishment_id}`))}
                        className={`flex items-center gap-3 px-4 py-3 mx-2 my-1 rounded-xl text-sm font-semibold transition-all duration-200 ${hoverClass}`}
                    >
                        <FaEye className="text-blue-500 opacity-80" /> {t("home.context-menu.details")}
                    </button>
                )}

                {serverConfig && serverConfig.isSecured && punishment.status === 'Active' && !isKick && (
                    <>
                        <button
                            onClick={() => handleAction(onEdit)}
                            className={`flex items-center gap-3 px-4 py-3 mx-2 my-1 rounded-xl text-sm font-semibold transition-all duration-200 ${hoverClass}`}
                        >
                            <FaEdit className="text-yellow-500 opacity-80" /> {t("home.context-menu.edit")}
                        </button>

                        {serverConfig.punishmentRevocation && (
                            <button
                                disabled={isRevoking}
                                onClick={() => handleAction(onRevoke)}
                                className={`flex items-center gap-3 px-4 py-3 mx-2 my-1 rounded-xl text-sm font-semibold transition-all duration-200 text-logout hover:bg-logout-hover`}
                            >
                                {isRevoking ? (
                                    <FaSpinner className="animate-spin text-text-secondary" />
                                ) : (
                                    <FaTrash className="opacity-80" />
                                )}
                                {t("home.context-menu.revoke")}
                            </button>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default PunishmentContextMenu;