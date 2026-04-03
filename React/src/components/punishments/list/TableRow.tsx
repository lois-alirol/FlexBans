import React from 'react';
import { Link } from 'react-router-dom';

import type { Punishment } from '@/types/punishments';

import PlayerHead from '@components/common/PlayerHead';
import StatusBadge from '@components/common/StatusBadge';

interface TableRowProps {
    punishment: Punishment;
    showExtendedInfo: boolean;
    onContextMenu: (e: React.MouseEvent, punishment: Punishment) => void;
    onRowClick: () => void;
}

const TableRow: React.FC<TableRowProps> = ({
                                               punishment,
                                               showExtendedInfo,
                                               onContextMenu,
                                               onRowClick
                                           }) => {
    const rowClasses = 'hover:bg-table-row-hover transition-all duration-200 cursor-pointer group'
    const cell = 'px-4 py-4 text-sm align-middle whitespace-nowrap text-center';
    const stop = (e: React.MouseEvent) => e.stopPropagation();

    return (
        <tr
            className={rowClasses}
            onClick={onRowClick}
            onContextMenu={(e) => onContextMenu(e, punishment)}
        >
            <td className={`${cell} font-semibold opacity-80 group-hover:opacity-100`}>
                {punishment.database_id}
            </td>

            <td className={cell} onClick={stop}>
                <div className="flex items-center justify-start gap-3 w-full opacity-80 group-hover:opacity-100 transition-opacity">
                    <div className="shrink-0 transition-transform group-hover:scale-110 duration-300">
                        <PlayerHead username={punishment.player} size={32} />
                    </div>
                    <Link
                        to={`/player/${punishment.player}`}
                        className="hover:underline truncate font-medium"
                        title={punishment.player}
                    >
                        {punishment.player}
                    </Link>
                </div>
            </td>

            <td className={cell} onClick={stop}>
                <div className="flex items-center justify-start gap-3 w-full opacity-80 group-hover:opacity-100 transition-opacity">
                    <div className="shrink-0">
                        <PlayerHead username={punishment.moderator} size={32} />
                    </div>
                    <Link
                        to={`/moderator/${punishment.moderator}`}
                        className="hover:underline truncate"
                        title={punishment.moderator}
                    >
                        {punishment.moderator}
                    </Link>
                </div>
            </td>

            <td className={`${cell} truncate opacity-80 group-hover:opacity-100 transition-opacity`} title={punishment.reason}>
                {punishment.reason}
            </td>

            <td className={`${cell} text-xs opacity-70 group-hover:opacity-90 transition-opacity`}>
                {punishment.date}
            </td>

            {showExtendedInfo && (
                <>
                    <td
                        className={`${cell} truncate text-xs opacity-80 group-hover:opacity-100`}
                        title={punishment.duration || '-'}
                    >
                        {punishment.duration || '-'}
                    </td>
                    <td className={`${cell} pr-6`}>
                        <div className="flex justify-center items-center transform scale-90 transition-transform group-hover:scale-100">
                            <StatusBadge status={punishment.status} className="text-xs font-medium px-2 py-0.5" />
                        </div>
                    </td>
                </>
            )}
        </tr>
    );
};

export default TableRow;