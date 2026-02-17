import React from 'react';
import { FaSearch } from 'react-icons/fa';
import type { Punishment } from '../../types/punishments';
import HistoryTableRow from './HistoryTableRow';

type HistoryContext = 'player' | 'moderator';

interface HistoryPunishmentTableProps {
  punishments: Punishment[];
  currentTheme: 'light' | 'dark';
  contextType: HistoryContext;
}

const HistoryPunishmentTable: React.FC<HistoryPunishmentTableProps> = ({ punishments, currentTheme, contextType }) => {
  const tableClasses = currentTheme === 'dark' ? 'bg-[#2c2c2c] border-gray-700' : 'bg-white border-gray-200';
  const headerClasses = currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-300' : 'bg-gray-100 text-gray-600';
  const dividerClasses = currentTheme === 'dark' ? 'divide-gray-700' : 'divide-gray-200';
  const rowHoverClasses = currentTheme === 'dark' ? 'hover:bg-[#333333]' : 'hover:bg-[#f3f3f3]';

  const associatedUserHeader = contextType === 'player' ? 'Moderator' : 'Player';

  if (punishments.length === 0) {
    return (
        <div className={`p-16 rounded-xl text-center border-2 border-dashed ${currentTheme === 'dark' ? 'border-gray-800 bg-[#242424]' : 'border-gray-200 bg-gray-50/50'}`}>
          <div className="flex justify-center mb-4">
            <div className={`p-4 rounded-full ${currentTheme === 'dark' ? 'bg-gray-800 text-gray-600' : 'bg-gray-200 text-gray-400'}`}>
              <FaSearch size={32} />
            </div>
          </div>
          <h3 className="text-xl font-bold mb-1">No Records Found</h3>
          <p className="opacity-50 max-w-xs mx-auto text-sm">No recorded punishments were found for this user.</p>
        </div>
    );
  }

  return (
      <div className={`rounded-xl shadow-xl border overflow-hidden ${tableClasses}`}>
        <div className="overflow-x-auto">
          <table className={`w-full table-fixed divide-y ${dividerClasses}`}>
            <colgroup>
              <col className="w-20" />
              <col className="w-[15%]" />
              <col className="w-[20%]" />
              <col className="w-[25%]" />
              <col className="w-[15%]" />
              <col className="w-[12%]" />
              <col className="w-[13%]" />
            </colgroup>

            <thead className={headerClasses}>
            <tr>
              <th className="px-6 py-4 text-center text-xs font-bold uppercase tracking-wider">ID</th>
              <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider">Type</th>
              <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider">{associatedUserHeader}</th>
              <th className="px-6 py-4 text-left text-xs font-bold uppercase tracking-wider">Reason</th>
              <th className="px-6 py-4 text-center text-xs font-bold uppercase tracking-wider">Date</th>
              <th className="px-6 py-4 text-center text-xs font-bold uppercase tracking-wider">Duration</th>
              <th className="px-6 py-4 text-center text-xs font-bold uppercase tracking-wider">Status</th>
            </tr>
            </thead>

            <tbody className={`divide-y ${dividerClasses}`}>
              {punishments.map((punishment) => (
                  <HistoryTableRow
                      key={punishment.database_id}
                      punishment={punishment}
                      contextType={contextType}
                      rowHoverClasses={rowHoverClasses}
                  />
              ))}
            </tbody>
          </table>
        </div>
      </div>
  );
};

export default HistoryPunishmentTable;