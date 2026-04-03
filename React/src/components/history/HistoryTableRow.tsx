import React from 'react';
import { useNavigate, Link } from 'react-router-dom';

import type { Punishment } from '@/types/punishments';

import PlayerHead from '@components/common/PlayerHead';
import StatusBadge from '@components/common/StatusBadge';

type HistoryContext = 'player' | 'moderator';

interface HistoryTableRowProps {
    punishment: Punishment;
    contextType: HistoryContext;
}

const HistoryTableRow: React.FC<HistoryTableRowProps> = ({
                                                             punishment,
                                                             contextType,
                                                         }) => {
    const navigate = useNavigate();

    const rowClasses = 'hover:bg-table-row-hover transition-all duration-200 cursor-pointer group';

    const cell = 'px-6 py-4 text-sm align-middle whitespace-nowrap text-center';
    const stop = (e: React.MouseEvent) => e.stopPropagation();

    const isKick = punishment.type?.toLowerCase().includes('kick');
    const associatedName = contextType === 'player' ? punishment.moderator : punishment.player;
    const profilePath = contextType === 'player' ? `/moderator/${associatedName}` : `/player/${associatedName}`;

    return (
        <tr
            onClick={() => navigate(`/punishment/${punishment.punishment_id}`)}
            className={rowClasses}
        >
            <td className={`${cell} font-semibold opacity-80 group-hover:opacity-100`}>
                {punishment.database_id}
            </td>

            <td className={cell}>
                <span className="bg-badge-type-background text-badge-type-text px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wider opacity-80 group-hover:opacity-100 transition-opacity">
                    {punishment.type}
                </span>
            </td>

            <td className={cell} onClick={stop}>
                <div className="flex items-center justify-start gap-3 w-full max-w-full opacity-80 group-hover:opacity-100 transition-opacity">
                    <div className="shrink-0 transition-transform group-hover:scale-110 duration-300">
                        <PlayerHead username={associatedName} size={32} />
                    </div>
                    <Link
                        to={profilePath}
                        className="hover:underline truncate min-w-0 font-medium"
                        title={associatedName}
                    >
                        {associatedName}
                    </Link>
                </div>
            </td>

            <td
                className={`${cell} truncate opacity-80 group-hover:opacity-100 transition-opacity max-w-[150px] text-left`}
                title={punishment.reason}
            >
                {punishment.reason}
            </td>

            <td className={`${cell} text-xs opacity-70 group-hover:opacity-90 transition-opacity`}>
                {punishment.date}
            </td>

            <td
                className={`${cell} truncate max-w-[120px] text-xs opacity-80 group-hover:opacity-100`}
                title={punishment.duration || '-'}
            >
                {isKick ? '-' : (punishment.duration || '-')}
            </td>

            <td className={`${cell} pr-12`}>
                <div className="flex justify-center items-center transform scale-90 transition-transform group-hover:scale-100">
                    {isKick ? (
                        <span className="text-gray-500 text-[10px] font-bold opacity-40 uppercase tracking-widest">
                            N/A
                        </span>
                    ) : (
                        <StatusBadge
                            status={punishment.status}
                            className="text-xs font-medium px-2 py-0.5"
                        />
                    )}
                </div>
            </td>
        </tr>
    );
};

export default HistoryTableRow;