import React from 'react';
import TableRow from './TableRow';
import type { Punishment, PunishmentType } from '../../types/punishments';

interface PunishmentTableProps {
  punishments: Punishment[];
  activeType: PunishmentType | null; 
  serverColor: string;
  currentTheme: 'dark' | 'light';
}

const PunishmentTable: React.FC<PunishmentTableProps> = ({ punishments, activeType, currentTheme }) => {
  const tableClasses = currentTheme === 'dark' ? 'bg-[#2c2c2c] border-gray-700' : 'bg-white border-gray-200';
  const headerClasses = currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-300' : 'bg-gray-100 text-gray-600';
  const dividerClasses = currentTheme === 'dark' ? 'divide-gray-700' : 'divide-gray-200';
  
  const punishmentTypeDisplay = activeType ? `${activeType}S` : 'Punishments';

  if (punishments.length === 0) {
    return (
      <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'}`}>
        <h3 className="text-xl font-semibold mb-2">No {punishmentTypeDisplay} Found</h3>
        <p className="text-gray-500">Try adjusting your search or filter settings.</p>
      </div>
    );
  }

  return (
    <div className={`rounded-xl shadow-xl border ${tableClasses}`}>
      <table className={`min-w-full divide-y ${dividerClasses}`}>
        <thead className={headerClasses}>
          <tr>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider rounded-tl-xl">ID</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Player</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Moderator</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Reason</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Date</th>
            <th className="px-6 py-3 text-left text-xs font-bold uppercase tracking-wider">Duration</th>
            <th className="px-6 py-3 text-center text-xs font-bold uppercase tracking-wider">Status</th>
            <th className="px-6 py-3 text-right text-xs font-bold uppercase tracking-wider rounded-tr-xl">Actions</th>
          </tr>
        </thead>
        <tbody className={`divide-y ${dividerClasses}`}>
          {punishments.map(p => (
            <TableRow key={p.id} punishment={p} currentTheme={currentTheme} />
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default PunishmentTable;