import React from 'react';

import type { Punishment } from '@/types/punishments';

import HistoryPunishmentTable from '@components/history/HistoryPunishmentTable';
import Pagination from '@components/common/Pagination';
import {useTranslation} from "react-i18next";

type HistoryContext = 'player' | 'moderator';

interface DetailedLogsSectionProps {
    filteredPunishments: Punishment[];
    totalPunishments: number;
    currentPage: number;
    totalPages: number;
    contextType: HistoryContext;
    onPageChange: (page: number) => void;
}

const DetailedLogsSection: React.FC<DetailedLogsSectionProps> = ({
                                                                     filteredPunishments,
                                                                     totalPunishments,
                                                                     currentPage,
                                                                     totalPages,
                                                                     contextType,
                                                                     onPageChange
                                                                 }) => {
    const { t } = useTranslation();

    return (
        <div className="mb-10">
            <div className="flex items-center gap-3 mb-4 px-2">
                <h2 className="text-xl font-bold">{t("history.table.title")}</h2>
                <div className="h-1 grow bg-linear-to-r from-transparent via-border-c to-transparent" />
                <span className="text-xs font-semibold px-2 py-1 rounded bg-surface opacity-60 uppercase">
                    {t("history.table.results-amount", {
                        count: filteredPunishments.length,
                        count_max: totalPunishments
                    })}
                </span>
            </div>
            <HistoryPunishmentTable
                punishments={filteredPunishments}
                contextType={contextType}
            />
            {totalPages > 1 && (
                <div className="mt-0 rounded-b-2xl p-4 flex items-center justify-between">
                    <div className="w-full">
                        <Pagination
                            currentPage={currentPage}
                            totalPages={totalPages}
                            onPageChange={onPageChange}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default DetailedLogsSection;