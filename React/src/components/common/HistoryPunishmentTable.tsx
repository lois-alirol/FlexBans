import React from 'react';
import type { Punishment } from '../../types/punishments';

type HistoryContext = 'player' | 'moderator';

interface HistoryPunishmentTableProps {
  punishments: Punishment[];
  currentTheme: 'light' | 'dark';
  contextType: HistoryContext; 
}

const getStatusClasses = (status: string) => {
  switch (status) {
    case 'Active':
      return 'bg-red-500/10 text-red-500 ring-red-500/20';
    case 'Expired':
      return 'bg-yellow-500/10 text-yellow-500 ring-yellow-500/20';
    case 'Removed':
      return 'bg-green-500/10 text-green-500 ring-green-500/20';
    default:
      return 'bg-gray-500/10 text-gray-500 ring-gray-500/20';
  }
};

const HistoryPunishmentTable: React.FC<HistoryPunishmentTableProps> = ({ punishments, currentTheme, contextType }) => {
  const tableClasses = currentTheme === 'dark' ? 'bg-[#2c2c2c] border-gray-700' : 'bg-white border-gray-200';
  const headerClasses = currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-300' : 'bg-gray-100 text-gray-600';
  const dividerClasses = currentTheme === 'dark' ? 'divide-gray-700' : 'divide-gray-200';
  const tableRowClasses = currentTheme === 'dark' ? 'hover:bg-[#383838] text-gray-300' : 'hover:bg-gray-50 text-gray-700';
  const emptyStateClasses = currentTheme === 'dark' ? 'border-gray-700 bg-[#242424] text-white' : 'border-gray-300 bg-white text-gray-800';

  const associatedUserHeader = contextType === 'player' ? 'Moderator' : 'Player';
  
  const emptyStateText = contextType === 'player' 
    ? 'This player has no recorded punishments.' 
    : 'This moderator has no recorded actions.';


  if (punishments.length === 0) {
    return (
      <div className={`p-10 rounded-xl text-center border border-dashed ${emptyStateClasses}`}>
        <h3 className="text-xl font-semibold mb-2">No Punishments Found</h3>
        <p className={`${currentTheme === 'dark' ? 'text-gray-400' : 'text-gray-500'}`}>{emptyStateText}</p>
      </div>
    );
  }

  return (
    <div className={`rounded-xl shadow-xl border ${tableClasses}`}>
      <table className={`min-w-full divide-y ${dividerClasses}`}>
        <thead className={headerClasses}>
          <tr>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider rounded-tl-xl">Type</th>
            {/* Dynamic Header */}
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">{associatedUserHeader}</th> 
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Reason</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Duration</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Date</th>
            <th className="px-6 py-3 text-center text-xs font-bold uppercase tracking-wider rounded-tr-xl">Status</th>
          </tr>
        </thead>
        <tbody className={`divide-y ${dividerClasses}`}>
          {punishments.map((punishment) => (
            <tr key={punishment.id} className={tableRowClasses}> 
              <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">{punishment.type}</td>
              <td className="px-6 py-4 whitespace-nowrap text-sm">
                {contextType === 'player' ? punishment.moderator : punishment.player}
              </td>
              <td className="px-6 py-4 text-sm max-w-xs truncate">{punishment.reason}</td>
              <td className="px-6 py-4 whitespace-nowrap text-sm">{punishment.duration}</td>
              <td className="px-6 py-4 whitespace-nowrap text-sm">{punishment.date}</td>
              <td className="px-6 py-4 whitespace-nowrap text-center">
                <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ring-1 ring-inset ${getStatusClasses(punishment.status)}`}>
                  {punishment.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default HistoryPunishmentTable;