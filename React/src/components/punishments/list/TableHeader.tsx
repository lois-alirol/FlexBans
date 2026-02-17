import React from 'react';

interface TableHeaderProps {
    showExtendedInfo: boolean;
    currentTheme: 'dark' | 'light';
}

const TableHeader: React.FC<TableHeaderProps> = ({ showExtendedInfo, currentTheme }) => {
    const headerClasses = currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-300' : 'bg-gray-100 text-gray-600';

    return (
        <thead className={headerClasses}>
        <tr>
            <th className="px-4 py-4 text-center text-xs font-bold uppercase tracking-wider rounded-tl-xl">ID</th>
            <th className="px-4 py-4 text-center text-xs font-bold uppercase tracking-wider">Player</th>
            <th className="hidden sm:table-cell px-4 py-4 text-center text-xs font-bold uppercase tracking-wider">Moderator</th>
            <th className="px-4 py-4 text-center text-xs font-bold uppercase tracking-wider">Reason</th>
            <th className="px-4 py-4 text-center text-xs font-bold uppercase tracking-wider">Date</th>
            {showExtendedInfo && (
                <>
                    <th className="hidden md:table-cell px-4 py-4 text-center text-xs font-bold uppercase tracking-wider">Duration</th>
                    <th className="hidden md:table-cell pl-4 pr-12 py-4 text-center text-xs font-bold uppercase tracking-wider rounded-tr-xl">
                        Status
                    </th>
                </>
            )}
        </tr>
        </thead>
    );
};

export default TableHeader;