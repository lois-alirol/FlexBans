import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import type { Punishment } from '../../types/punishments';
import PlayerHead from "../common/PlayerHead";
import StatusBadge from '../common/StatusBadge';

type HistoryContext = 'player' | 'moderator';

interface HistoryTableRowProps {
    punishment: Punishment;
    contextType: HistoryContext;
    rowHoverClasses: string;
}

const HistoryTableRow: React.FC<HistoryTableRowProps> = ({
                                                             punishment,
                                                             contextType,
                                                             rowHoverClasses
                                                         }) => {
    const navigate = useNavigate();

    const isKick = punishment.type?.toLowerCase().includes('kick');
    const associatedName = contextType === 'player' ? punishment.moderator : punishment.player;
    const profilePath = contextType === 'player' ? `/moderator/${associatedName}` : `/player/${associatedName}`;

    return (
        <tr
            onClick={() => navigate(`/punishment/${punishment.punishment_id}`)}
            className={`${rowHoverClasses} transition cursor-pointer`}
        >
            <td className="px-6 py-4 text-sm font-semibold text-center whitespace-nowrap">
                {punishment.database_id}
            </td>

            <td className="px-6 py-4 text-sm font-bold whitespace-nowrap">
                <span className="bg-blue-500/10 text-blue-500 px-2 py-1 rounded text-[10px] uppercase">
                  {punishment.type}
                </span>
            </td>

            <td className="px-6 py-4 text-sm whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center gap-3">
                    <PlayerHead username={associatedName} size={32} />
                    <Link to={profilePath} className="hover:underline font-semibold">
                        {associatedName}
                    </Link>
                </div>
            </td>

            <td className="px-6 py-4 text-sm truncate max-w-xs text-gray-500 dark:text-gray-400" title={punishment.reason}>
                {punishment.reason}
            </td>

            <td className="px-6 py-4 text-sm text-center whitespace-nowrap">
                {punishment.date}
            </td>

            <td className="px-6 py-4 text-sm text-center whitespace-nowrap">
                {isKick ? <span className="opacity-30">N/A</span> : (punishment.duration || '-')}
            </td>

            <td className="px-6 py-4 text-center whitespace-nowrap">
                {isKick ? (
                    <span className="text-gray-500 text-xs opacity-40">N/A</span>
                ) : (
                    <div className="flex justify-center">
                        <StatusBadge
                            status={punishment.status}
                            className="text-[10px] font-black uppercase tracking-widest px-2.5 py-1"
                        />
                    </div>
                )}
            </td>
        </tr>
    );
};

export default HistoryTableRow;