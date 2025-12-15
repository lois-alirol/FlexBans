import React, { useState, useMemo, useCallback } from 'react';
import { FaSearch, FaFilter, FaChartBar, FaUserCheck, FaUserTimes } from 'react-icons/fa';
import { useLocation, useParams } from 'react-router-dom';
import Pagination from '../components/common/Pagination'; 
import type { Punishment } from '../types/punishments';

import { usePunishmentsData } from '../hooks/usePunishmentsData'; 
import type { PlayerStats, StaffStats } from '../types/punishments';

import BackButton from '../components/common/BackButton';
import HistoryPunishmentTable from '../components/common/HistoryPunishmentTable';
import FilterModal from '../components/punishments/FilterModal';
import { useServerConfig } from '../hooks/useServerConfig';
import { useTheme } from '../hooks/useTheme';

type Theme = 'dark' | 'light';
type HistoryContext = 'player' | 'moderator';

interface EmptyStateContentProps {
  searchTerm: string; 
  associatedUser: string;
  currentTheme: Theme;
  contextType: HistoryContext; 
}

const EmptyStateContent: React.FC<EmptyStateContentProps> = ({ 
  searchTerm, 
  associatedUser, 
  currentTheme,
  contextType
}) => {
    const primaryUserLabel = contextType === 'player' ? 'Player' : 'Moderator';

    const message = searchTerm && searchTerm.length > 0
        ? `No punishments by ${contextType === 'player' ? 'moderator' : 'player'} "${searchTerm}" for ${primaryUserLabel} "${associatedUser}".`
        : `${primaryUserLabel} ${associatedUser} has no recent actions.`;

    return (
        <div className={`p-10 rounded-xl text-center border border-dashed mt-8 ${currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'}`}>
            <h3 className="text-xl font-semibold mb-2">No Punishments Found</h3>
            <p className="text-gray-500">
                {message}
            </p>
        </div>
    );
};

interface StatCardProps {
    title: string;
    value: number;
    icon: React.ReactNode;
    colorClass: string;
    currentTheme: Theme;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, colorClass, currentTheme }) => {
    const cardBg = currentTheme === 'dark' ? 'bg-[#282828]' : 'bg-white';
    const shadow = 'shadow-lg';

    return (
        <div className={`p-5 rounded-xl ${cardBg} ${shadow} flex items-center justify-between transition-all duration-300 transform hover:scale-[1.02] border border-transparent ${currentTheme === 'dark' ? 'dark:hover:border-gray-600' : 'hover:border-gray-300'}`}>
            <div>
                <p className="text-sm font-medium opacity-70 mb-1">{title}</p>
                <p className="text-3xl font-extrabold">{value}</p>
            </div>
            <div className={`p-3 rounded-full text-2xl ${colorClass}`}>
                {icon}
            </div>
        </div>
    );
};

const HistoryPage: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { currentTheme } = useTheme();
  
  const { punishmentsData, isLoading, error } = usePunishmentsData();
  const { recentPunishments, userStats } = punishmentsData;

  const location = useLocation();
  const params = useParams();

  const isPlayerContext = location.pathname.startsWith('/player/');
  const contextType: HistoryContext = isPlayerContext ? 'player' : 'moderator';

  let itemsPerPage = 20;

  if (isPlayerContext) {
    itemsPerPage = serverConfig.histories?.playerMaxPerPage || 20;
  } else {
    itemsPerPage = serverConfig.histories?.moderatorMaxPerPage || 20;
  }

  const associatedUser = isPlayerContext ? params.playerName : params.moderatorName;
  const searchLabel = isPlayerContext ? 'Moderator Name' : 'Player Name';

  const [currentPage, setCurrentPage] = useState(1);
  const [searchTerm, setSearchTerm] = useState('');
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  
  const punishmentStats = useMemo(() => {
    const stats = associatedUser ? userStats[associatedUser] : undefined;

    if (stats) {
        return stats;
    }
    
    return isPlayerContext 
        ? ({ received: 0, active: 0, removed: 0, expired: 0 } as PlayerStats)
        : ({ sent: 0, active: 0, removed: 0, bansSent: 0 } as StaffStats);
    
  }, [associatedUser, isPlayerContext, userStats]);

  let statsToDisplay: { title: string; value: number; icon: React.ReactNode; colorClass: string }[] = [];

  if ('received' in punishmentStats) {
      const pStats = punishmentStats as PlayerStats;
      statsToDisplay = [
          { title: 'Total Received', value: pStats.received, icon: <FaChartBar />, colorClass: 'bg-indigo-500/20 text-indigo-400' },
          { title: 'Active', value: pStats.active, icon: <FaUserTimes />, colorClass: 'bg-red-500/20 text-red-400' },
          { title: 'Removed', value: pStats.removed, icon: <FaUserCheck />, colorClass: 'bg-green-500/20 text-green-400' },
          { title: 'Expired', value: pStats.expired, icon: <FaFilter />, colorClass: 'bg-yellow-500/20 text-yellow-400' },
      ];
  } else {
      const sStats = punishmentStats as StaffStats;
      statsToDisplay = [
          { title: 'Total Sent', value: sStats.sent, icon: <FaChartBar />, colorClass: 'bg-indigo-500/20 text-indigo-400' },
          { title: 'Bans Sent', value: sStats.bansSent, icon: <FaUserTimes />, colorClass: 'bg-red-500/20 text-red-400' },
          { title: 'Active (Mod)', value: sStats.active, icon: <FaUserCheck />, colorClass: 'bg-green-500/20 text-green-400' },
          { title: 'Removed (Mod)', value: sStats.removed, icon: <FaFilter />, colorClass: 'bg-yellow-500/20 text-yellow-400' },
      ];
  }

  const basePunishments: Punishment[] = useMemo(() => {
    if (!associatedUser || isLoading) return [];

    return recentPunishments.filter(p => 
      isPlayerContext ? p.player === associatedUser : p.moderator === associatedUser
    );
  }, [associatedUser, isPlayerContext, recentPunishments, isLoading]);

  const { filteredPunishments, totalPages } = useMemo(() => {
    if (!associatedUser) return { filteredPunishments: [], totalPages: 0 };
    
    const searchField = isPlayerContext ? 'moderator' : 'player';

    const searchFiltered = basePunishments.filter(p => {
        const valueToSearch = p[searchField as keyof Punishment] as string;
        return valueToSearch.toLowerCase().includes(searchTerm.toLowerCase());
    });

    const totalPages = Math.ceil(searchFiltered.length / itemsPerPage);

    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedPunishments = searchFiltered.slice(startIndex, endIndex);

    return { filteredPunishments: paginatedPunishments, totalPages };
  }, [basePunishments, searchTerm, currentPage, isPlayerContext, associatedUser, itemsPerPage]);

  const handlePageChange = useCallback((page: number) => {
    if (page > 0 && page <= totalPages) {
        setCurrentPage(page);
    }
  }, [totalPages]);

  const handleFilterApply = () => {
    setIsFilterModalOpen(false);
  };
  
  React.useEffect(() => {
      setCurrentPage(1);
  }, [searchTerm]);
  
  React.useEffect(() => {
      if (currentPage > totalPages && totalPages > 0) {
          setCurrentPage(1);
      }
  }, [totalPages, currentPage]);


  const bgColor = currentTheme === 'dark' ? 'bg-[#1c1c1c]' : 'bg-white';
  const textColor = currentTheme === 'dark' ? 'text-[#e0e0e0]' : 'text-[#333333]';

  if (!associatedUser) {
    return null;
  }

  const titleText = isPlayerContext 
    ? `${associatedUser} Recent Punishments` 
    : `${associatedUser} Action History`;

  if (error) {
      return (
          <div className={`min-h-screen ${bgColor} ${textColor} flex items-center justify-center`}>
              <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-red-700 bg-[#242424]' : 'border-red-400 bg-white'} shadow-xl`}>
                  <h3 className="text-xl font-semibold mb-2 text-red-500">Error Fetching History</h3>
                  <p className="text-gray-500">Could not load punishment history: {error}</p>
              </div>
          </div>
      );
  }

  if (isLoading) {
      return (
          <div className={`min-h-screen ${bgColor} ${textColor} flex items-center justify-center`}>
              <div className={`flex items-center space-x-3 p-8 rounded-xl ${currentTheme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white'} shadow-xl`}>
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2" style={{ borderColor: serverConfig.serverColor }}></div>
                  <p className="text-xl font-semibold">Loading History for {associatedUser}...</p>
              </div>
          </div>
      );
  }

  return (
    <div className={`min-h-screen ${bgColor} ${textColor}`}>
      <div className="max-w-7xl mx-auto p-5">
        
        <div className="mb-6">
          <BackButton to="/" currentTheme={currentTheme} />
        </div>
        
        <h1 className={`text-center text-3xl font-bold mb-8 ${textColor}`}>
          {titleText}
        </h1>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            {statsToDisplay.map((stat) => (
                <StatCard 
                    key={stat.title}
                    title={stat.title}
                    value={stat.value}
                    icon={stat.icon}
                    colorClass={stat.colorClass}
                    currentTheme={currentTheme}
                />
            ))}
        </div>
        
        <div className="search mb-8 flex items-center gap-2">
          <div className="relative grow">
            <input 
              type="text" 
              placeholder={`Search by ${searchLabel}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className={`w-full p-3.5 pl-12 rounded-xl shadow-lg focus:outline-none focus:ring-2 ${
                  currentTheme === 'dark' ? 'bg-[#2c2c2c] border-none text-white' : 'bg-white border border-gray-300 text-gray-800'
              } transition duration-300`}
              style={currentTheme !== 'dark' ? { '--tw-ring-color': serverConfig.serverColor } as React.CSSProperties : {}}
            />
            <FaSearch className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400" />
          </div>
          <button
            onClick={() => setIsFilterModalOpen(true)}
            className={`p-3.5 rounded-xl text-lg shadow-lg transition duration-300 transform hover:scale-[1.05] ${
              currentTheme === 'dark' ? 'bg-[#2c2c2c] text-white hover:bg-[#383838]' : 'bg-white text-gray-800 hover:bg-gray-100'
              }`}
            aria-label="Filter punishments"
          >
            <FaFilter />
          </button>
        </div>

        <div className="overflow-x-auto shadow-xl rounded-lg">
            {filteredPunishments.length > 0 ? (
                <HistoryPunishmentTable 
                    punishments={filteredPunishments}
                    currentTheme={currentTheme} 
                    contextType={contextType}                    
                />
            ) : (
                <EmptyStateContent 
                  searchTerm={searchTerm} 
                  associatedUser={associatedUser} 
                  currentTheme={currentTheme} 
                  contextType={contextType}
                />
            )}
        </div>
        
        {totalPages > 1 && (
            <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={handlePageChange}
                currentTheme={currentTheme}
            />
        )}

      </div>
      
      <FilterModal 
        isOpen={isFilterModalOpen} 
        onClose={() => setIsFilterModalOpen(false)} 
        onApply={handleFilterApply}
        serverColor={serverConfig.serverColor}
        currentTheme={currentTheme}
      />
    </div>
  );
};

export default HistoryPage;