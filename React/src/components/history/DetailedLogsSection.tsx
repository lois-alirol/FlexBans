import React from 'react';
import HistoryPunishmentTable from '../history/HistoryPunishmentTable';
import Pagination from '../common/Pagination';
import type { Punishment } from '../../types/punishments';

type HistoryContext = 'player' | 'moderator';

interface DetailedLogsSectionProps {
    filteredPunishments: Punishment[];
    totalPunishments: number;
    currentPage: number;
    totalPages: number;
    contextType: HistoryContext;
    currentTheme: 'light' | 'dark';
    onPageChange: (page: number) => void;
}

const DetailedLogsSection: React.FC<DetailedLogsSectionProps> = ({
                                                                     filteredPunishments,
                                                                     totalPunishments,
                                                                     currentPage,
                                                                     totalPages,
                                                                     contextType,
                                                                     currentTheme,
                                                                     onPageChange
                                                                 }) => {
    return (
        <div className="mb-10">
            <div className="flex items-center gap-3 mb-4 px-2">
                <h2 className="text-xl font-bold">Detailed Logs</h2>
                <div className="h-1 grow bg-linear-to-r from-transparent via-gray-500/10 to-transparent" />
                <span className="text-xs font-bold px-2 py-1 rounded bg-gray-500/10 opacity-60 uppercase">
                    {filteredPunishments.length} of {totalPunishments} results
                </span>
            </div>
            <HistoryPunishmentTable
                punishments={filteredPunishments}
                currentTheme={currentTheme}
                contextType={contextType}
            />
            {totalPages > 1 && (
                <div className="mt-0 rounded-b-2xl p-4 flex items-center justify-between">
                    <div className="w-full">
                        <Pagination
                            currentPage={currentPage}
                            totalPages={totalPages}
                            onPageChange={onPageChange}
                            currentTheme={currentTheme}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default DetailedLogsSection;