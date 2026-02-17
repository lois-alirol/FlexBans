import React, { useRef, useEffect } from 'react';
import { FaEye, FaTrash, FaEdit, FaSpinner } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import type { Punishment } from "../../../types/punishments.tsx";
import {useServerConfig} from "../../../hooks/useServerConfig.ts";

interface PunishmentContextMenuProps {
    position: { x: number; y: number };
    side: 'top' | 'bottom';
    punishment: Punishment;
    onClose: () => void;
    onRevoke: () => void;
    onEdit: () => void;
    currentTheme: 'dark' | 'light';
    isRevoking: boolean;
}

const PunishmentContextMenu: React.FC<PunishmentContextMenuProps> = ({
                                                                         position,
                                                                         side,
                                                                         punishment,
                                                                         onClose,
                                                                         onRevoke,
                                                                         onEdit,
                                                                         currentTheme,
                                                                         isRevoking,
                                                                     }) => {
    const menuRef = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();
    const { serverConfig } = useServerConfig();

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                onClose();
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    const bgClass = currentTheme === 'dark' ? 'bg-[#242424] border-gray-600' : 'bg-white border-gray-200';
    const textClass = currentTheme === 'dark' ? 'text-gray-200' : 'text-gray-700';
    const hoverClass = currentTheme === 'dark' ? 'hover:bg-[#333333]' : 'hover:bg-gray-100';

    const handleAction = (action: () => void) => {
        action();
        onClose();
    };

    const isKick = punishment.type.toLowerCase().includes('kick');
    const transformStyle = side === 'top' ? { transform: 'translateY(-18px)' } : { transform: 'translateY(calc(-100% + 18px))' };
    const arrowPlacement = side === 'top' ? 'top-3' : 'bottom-3';

    return (
        <div
            ref={menuRef}
            className={`absolute z-50 rounded-lg shadow-xl border w-48 py-2 ${bgClass}`}
            style={{ top: position.y, left: position.x + 15, ...transformStyle }}
        >
            <div className={`absolute -left-1.5 ${arrowPlacement} w-3 h-3 border-l border-b transform rotate-45 ${bgClass}`} />
            <div className="flex flex-col relative z-10">
                {serverConfig.detailsPageEnabled && (
                    <button onClick={() => handleAction(() => navigate(`/punishment/${punishment.punishment_id}`))} className={`flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors ${textClass} ${hoverClass}`}>
                        <FaEye className="text-blue-500" /> View Details
                    </button>
                )}

                {serverConfig.isSecured && punishment.status === 'Active' && !isKick && (
                    <>
                        <button onClick={() => handleAction(onEdit)} className={`flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors ${textClass} ${hoverClass}`}>
                            <FaEdit className="text-yellow-500" /> Edit Punishment
                        </button>

                        {serverConfig.punishmentRevocation && (
                            <button disabled={isRevoking} onClick={() => handleAction(onRevoke)} className={`flex items-center gap-3 px-4 py-3 text-sm font-medium transition-colors ${textClass} ${hoverClass}`}>
                                {isRevoking ? <FaSpinner className="animate-spin text-gray-500" /> : <FaTrash className="text-red-500" />}
                                Revoke
                            </button>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default PunishmentContextMenu;