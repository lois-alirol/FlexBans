import React from 'react';
import { FaSearch } from 'react-icons/fa';

import type { Punishment } from '@/types/punishments';

import HistoryTableRow from '@components/history/HistoryTableRow';
import {useTranslation} from "react-i18next";

type HistoryContext = 'player' | 'moderator';

interface HistoryPunishmentTableProps {
  punishments: Punishment[];
  contextType: HistoryContext;
}

const HistoryPunishmentTable: React.FC<HistoryPunishmentTableProps> = ({ punishments, contextType }) => {
  const {t } = useTranslation();

  const containerClasses = 'bg-table-row-background border-surface-border';
  const dividerClasses = 'divide-surface-border';
  const headerClasses = 'bg-surface-elevated text-text-secondary';

  const thClasses = "px-4 py-5 text-center text-[10px] font-bold uppercase tracking-[0.15em]";

  const associatedUserHeader = contextType === 'player' ? t("history.table.headers.moderator") : t("history.table.headers.player");

  if (punishments.length === 0) {
    return (
        <div className={`p-16 rounded-2xl text-center border-2 transition-all duration-300 border-border-c bg-surface`}>
          <div className="flex justify-center mb-4">
            <div className={`p-4 rounded-full bg-surface-elevated text-text-secondary`}>
              <FaSearch size={32} />
            </div>
          </div>
          <h3 className="text-xl font-bold mb-1">{t("history.table.no-results.title")}</h3>
          <p className="opacity-50 max-w-xs mx-auto text-sm">{t("history.table.no-results.description")}</p>
        </div>
    );
  }

  return (
      <div className={`rounded-2xl border backdrop-blur-xl overflow-hidden transition-all duration-300 shadow-xl ${containerClasses}`}>
        <div className="overflow-x-auto">
          <table className={`w-full table-fixed divide-y ${dividerClasses}`}>
            <colgroup>
              <col className="w-20" />
              <col className="w-[12%]" />
              <col className="w-[18%]" />
              <col className="w-[22%]" />
              <col className="w-[15%]" />
              <col className="w-[13%]" />
              <col className="w-[13%]" />
            </colgroup>

            <thead className={headerClasses}>
            <tr>
              <th className={thClasses}>{t("history.table.headers.id")}</th>
              <th className={thClasses}>{t("history.table.headers.type")}</th>
              <th className={thClasses}>{associatedUserHeader}</th>
              <th className={thClasses}>{t("history.table.headers.reason")}</th>
              <th className={thClasses}>{t("history.table.headers.date")}</th>
              <th className={thClasses}>{t("history.table.headers.duration")}</th>
              <th className={thClasses}>{t("history.table.headers.status")}</th>
            </tr>
            </thead>

            <tbody className={`divide-y ${dividerClasses}`}>
            {punishments.map((punishment) => (
                <HistoryTableRow
                    key={punishment.database_id}
                    punishment={punishment}
                    contextType={contextType}
                />
            ))}
            </tbody>
          </table>
        </div>
      </div>
  );
};

export default HistoryPunishmentTable;