import React from 'react';
import { Link } from 'react-router-dom';
import type {Punishment} from "../../../types/punishments.tsx";
import PlayerHead from "../../common/PlayerHead.tsx";
import StatusBadge from "../../common/StatusBadge.tsx";

interface TableRowProps {
    punishment: Punishment;
    currentTheme: 'dark' | 'light';
    showExtendedInfo: boolean;
    onContextMenu: (e: React.MouseEvent, punishment: Punishment) => void;
    onRowClick: () => void;
}

const TableRow: React.FC<TableRowProps> = ({
                                               punishment,
                                               currentTheme,
                                               showExtendedInfo,
                                               onContextMenu,
                                               onRowClick
                                           }) => {
    const isKick = punishment.type.toLowerCase().includes('kick');
    const rowClasses = currentTheme === 'dark'
        ? 'hover:bg-[#333333] transition cursor-pointer'
        : 'hover:bg-[#f3f3f3] transition cursor-pointer';

    // Added max-w-0 to the cell class to help with flex truncation in table-fixed layouts
    const cell = 'px-6 py-4 text-sm align-middle whitespace-nowrap text-center';
    const muted = 'text-gray-500 dark:text-gray-400';
    const stop = (e: React.MouseEvent) => e.stopPropagation();

    return (
        <tr
            className={rowClasses}
            onClick={onRowClick}
            onContextMenu={(e) => onContextMenu(e, punishment)}
        >
            <td className={`${cell} font-semibold`}>{punishment.database_id}</td>

            {/* Player Column */}
            <td className={cell} onClick={stop}>
                <div className="flex items-center justify-start gap-3 w-full max-w-full">
                    {/* shrink-0 prevents the head from being cut off */}
                    <div className="shrink-0">
                        <PlayerHead username={punishment.player} size={32} />
                    </div>
                    {/* truncate and min-w-0 allow the name to be cut off if needed */}
                    <Link
                        to={`/player/${punishment.player}`}
                        className="hover:underline truncate min-w-0"
                        title={punishment.player}
                    >
                        {punishment.player}
                    </Link>
                </div>
            </td>

            {/* Moderator Column - Applied same logic for consistency */}
            <td className={`${cell} ${muted} hidden sm:table-cell`} onClick={stop}>
                <div className="flex items-center justify-start gap-3 w-full max-w-full">
                    <div className="shrink-0">
                        <PlayerHead username={punishment.moderator} size={32} />
                    </div>
                    <Link
                        to={`/moderator/${punishment.moderator}`}
                        className="hover:underline truncate min-w-0"
                        title={punishment.moderator}
                    >
                        {punishment.moderator}
                    </Link>
                </div>
            </td>

            <td className={`${cell} truncate max-w-[150px]`} title={punishment.reason}>
                {punishment.reason}
            </td>

            <td className={cell}>{punishment.date}</td>

            {showExtendedInfo && (
                <>
                    <td
                        className={`${cell} hidden md:table-cell truncate max-w-[120px]`}
                        title={isKick ? 'N/A' : punishment.duration || '-'}
                    >
                        {isKick ? 'N/A' : punishment.duration || '-'}
                    </td>
                    <td className={`${cell} pr-12 hidden md:table-cell`}>
                        {isKick ? (
                            <span className="text-gray-500 text-xs">N/A</span>
                        ) : (
                            <div className="flex justify-center items-center">
                                <StatusBadge status={punishment.status} className="text-xs font-medium px-2 py-0.5" />
                            </div>
                        )}
                    </td>
                </>
            )}
        </tr>
    );
};

export default TableRow;