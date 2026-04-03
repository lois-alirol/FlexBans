import React from "react";
import type {PunishmentType} from "@/types/punishments";

const NavButton: React.FC<{
    type: PunishmentType;
    activeType: PunishmentType | null;
    count: number;
    icon: React.ReactNode;
    onClick: () => void;
    isExpanded: boolean;
    disabled?: boolean;
}> = ({ type, activeType, count, icon, onClick, isExpanded, disabled }) => {
    const isActive = type === activeType;

    return (
        <button
            onClick={!disabled ? onClick : undefined}
            disabled={disabled}
            className={`group relative flex items-center rounded-xl border transition-all duration-200 mb-2
                ${isExpanded ? 'w-full px-4 py-3' : 'w-12 h-12 justify-center'}
                ${disabled ? 'opacity-30 cursor-not-allowed' : 'cursor-pointer hover:-translate-y-px'}
                ${isActive ? 'bg-server-color/80 border-transparent text-sidebar-text-active' : 'border-border-c bg-sidebar-surface text-sidebar-text-disabled hover:border-border-active'}
            `}
        >
            <div className="shrink-0 text-lg flex items-center justify-center">{icon}</div>
            {isExpanded && (
                <>
                    <span className="ml-3 font-bold text-sm uppercase tracking-wide animate-in slide-in-from-left-2">{type}S</span>
                    <span className={`ml-auto text-[10px] font-black px-2 py-0.5 rounded-lg ${isActive ? 'bg-sidebar-surface-elevated-active text-sidebar-text-active' : 'bg-sidebar-surface-elevated text-sidebar-text-disabled'}`}>
                        {count}
                    </span>
                </>
            )}
        </button>
    );
};

export default NavButton;