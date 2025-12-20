import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { FaBars, FaSearch, FaFilter } from 'react-icons/fa';
import PunishmentTable from '../components/punishments/PunishmentTable';
import FilterModal from '../components/punishments/FilterModal';
import Sidebar from '../components/punishments/Sidebar';
import Pagination from '../components/common/Pagination';
import type { PunishmentType } from '../types/punishments';
import NewPunishmentModal from '../components/punishments/NewPunishmentModal';
import MyAccount from '../components/common/MyAccount';
import { useAuth } from '../hooks/useAuth';
import { useServerConfig } from '../hooks/useServerConfig';
import { usePunishmentsData } from '../hooks/usePunishmentsData';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../hooks/useTheme';

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  });
};

interface PunishmentSetting {
    enabled: boolean;
    maxPerPage: number;
}

const PunishmentsPage: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();
  const { isDarkMode } = useTheme();
  const navigate = useNavigate();
  
  const { punishmentsData, isLoading, error } = usePunishmentsData();
  const { recentPunishments, globalCounts } = punishmentsData;

  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [isNewPunishmentModalOpen, setIsNewPunishmentModalOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeType, setActiveType] = useState<PunishmentType | null>(null);
  
  useEffect(() => {
    if (isAuthLoading || serverConfig.serverName === 'Loading...') {
      return; 
    }

    if (serverConfig.isSecured && !isAuthenticated && !isAuthLoading) {
      navigate('/login');
    }
  
  }, [serverConfig.isSecured, isAuthenticated, isAuthLoading, serverConfig.serverName, navigate]);


  const handlePageChange = useCallback((page: number) => {
    setCurrentPage(page);
  }, []);

  useEffect(() => {
    if (currentPage >= 1) {
      scrollToTop();
    }
  }, [currentPage]);

  useEffect(() => {
    const resetPage = async () => {
      setCurrentPage(1);
    };

    resetPage();
  }, [activeType, searchTerm]);

  useEffect(() => {
    const updateActiveType = async () => {
      const punishmentSettings = Object.values(serverConfig.punishments) as PunishmentSetting[];
      const isAnyEnabled = punishmentSettings.some(p => p.enabled);

      if (!isAnyEnabled && activeType !== null) {
        setActiveType(null);
      } else if (isAnyEnabled && !activeType) {
        const firstEnabledKey = Object.entries(serverConfig.punishments)
          .find(([, p]) => p.enabled)?.[0];

        if (firstEnabledKey) {
          const singularKey = firstEnabledKey.endsWith('s')
            ? firstEnabledKey.slice(0, -1)
            : firstEnabledKey;

          setActiveType(singularKey.toUpperCase() as PunishmentType);
        }
      }
    };

    updateActiveType();
  }, [activeType, serverConfig]);

  const { paginatedPunishments, totalPages } = useMemo(() => {
    let punishments = recentPunishments; 
    
    if (isLoading || isAuthLoading || serverConfig.serverName === 'Loading...') {
        return { paginatedPunishments: [], totalPages: 1 };
    }

    if (activeType !== null) {
      punishments = punishments.filter(p => p.type === activeType);
    }
    
    const filteredPunishments = punishments.filter(p => 
      p.player.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const typeKey = activeType ? activeType.toLowerCase() as keyof typeof serverConfig.punishments : null;
    
    const itemsPerPage = typeKey && serverConfig.punishments[typeKey]?.enabled 
      ? serverConfig.punishments[typeKey].maxPerPage
      : 20;

    const totalPages = Math.ceil(filteredPunishments.length / itemsPerPage);
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedPunishments = filteredPunishments.slice(startIndex, endIndex);

    return { paginatedPunishments, totalPages };
  }, [activeType, searchTerm, currentPage, serverConfig, recentPunishments, isLoading, isAuthLoading]);

  useEffect(() => {
    const updatePage = async () => {
      let newPage = currentPage;

      if (totalPages > 0 && currentPage > totalPages) {
        newPage = totalPages;
      } else if (currentPage === 0) {
        newPage = 1;
      }

      if (newPage !== currentPage) {
        setCurrentPage(newPage);
      }
    };

    updatePage();
  }, [currentPage, totalPages]);

  
  const handleFilterApply = () => {
    setIsFilterModalOpen(false);
  };

  const handleOpenNewPunishmentModal = () => {
    setIsNewPunishmentModalOpen(true);
  };

  const handleCloseNewPunishmentModal = () => {
    setIsNewPunishmentModalOpen(false);
  };

  const isAnyPunishmentEnabled = (Object.values(serverConfig.punishments) as PunishmentSetting[]).some(p => p.enabled);  
  const currentTheme = isDarkMode ? 'dark' : 'light';
  const typeDisplay = activeType ? `${activeType}S` : 'All Punishments';
  const searchPlaceholder = activeType ? `Search Player for ${activeType}...` : 'Search Player...';
  const titleActiveType = activeType || 'Overview';

  const renderContent = () => {
    if (isAuthLoading || serverConfig.serverName === 'Loading...') {
        return (
             <div className="absolute inset-0 flex items-center justify-center backdrop-blur-sm z-50">
                <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'}`}>
                    <h3 className="text-xl font-semibold mb-2">Checking Access...</h3>
                    <p className="text-gray-500">Determining server security requirements.</p>
                    <div className="mt-4 flex justify-center">
                        <div className={`animate-spin rounded-full h-8 w-8 border-b-2`} style={{ borderColor: serverConfig.serverColor }}></div>
                    </div>
                </div>
            </div>
        );
    }
    
    if (serverConfig.isSecured && !isAuthenticated) {
        return null; 
    }

    if (error) {
        return (
            <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-red-700 bg-[#242424]' : 'border-red-300 bg-white'}`}>
                <h3 className="text-xl font-semibold mb-2 text-red-500">Error Fetching Data</h3>
                <p className="text-gray-500">Could not load punishments from the API: {error}</p>
            </div>
        );
    }

    if (isLoading) {
        return (
            <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'}`}>
                <h3 className="text-xl font-semibold mb-2">Loading Punishments...</h3>
                <p className="text-gray-500">Please wait while we fetch the latest data.</p>
                <div className="mt-4 flex justify-center">
                    <div className={`animate-spin rounded-full h-8 w-8 border-b-2`} style={{ borderColor: serverConfig.serverColor }}></div>
                </div>
            </div>
        );
    }

    return null;
  }

  if (isAuthLoading || (serverConfig.isSecured && !isAuthenticated)) {
      return (
        <div className={`flex min-h-screen items-center justify-center ${currentTheme === 'dark' ? 'bg-[#1c1c1c] text-[#e0e0e0]' : 'bg-[#f4f4f4] text-[#333333]'}`}>
            {renderContent()}
        </div>
      );
  }

  return (
    <div className={`flex min-h-screen ${currentTheme === 'dark' ? 'bg-[#1c1c1c] text-[#e0e0e0]' : 'bg-[#f4f4f4] text-[#333333]'}`}>
      
      <title>{serverConfig.serverName} Punishments - {titleActiveType}</title>

      <Sidebar
        serverConfig={serverConfig}
        counts={globalCounts}
        activeType={activeType}
        setActiveType={setActiveType}
        isSidebarOpen={isSidebarOpen}
        setIsSidebarOpen={setIsSidebarOpen}
        onNewPunishmentClick={handleOpenNewPunishmentModal}     
      />

      <div className={`flex flex-col grow transition-all duration-300 ${isSidebarOpen ? 'lg:ml-64' : 'lg:ml-20'}`}>
        <header className={`flex items-center justify-between p-4 ${currentTheme === 'dark' ? 'bg-[#242424]' : 'bg-[#e4e4e4]'} lg:hidden shadow-md`}>
          <button onClick={() => setIsSidebarOpen(true)} aria-label="Open menu" className="text-xl">
            <FaBars />
          </button>
          <img src={serverConfig.serverLogo} alt="Logo" className="h-6" />
        </header>

        <main className="p-4 sm:p-6 lg:p-8">
          <div className="flex justify-between items-start">
            <div>
              <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
                {serverConfig.serverName}'s Punishments
              </h1>
            </div>

            {serverConfig.isSecured && isAuthenticated &&
              <div className="hidden lg:block mt-1"> 
                  <MyAccount currentTheme={currentTheme} />
              </div>
            }
          </div>

          <p className="text-sm sm:text-base mb-6 text-gray-500 dark:text-gray-400">
            Viewing <span className={`font-semibold ${currentTheme === 'dark' ? 'text-white' : 'text-black'}`}>{typeDisplay}</span>. Select a different type from the menu.
          </p>

          <div className="search mb-8 flex items-center gap-2">
            <div className="relative grow">
              <input 
                type="text" 
                placeholder={searchPlaceholder}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className={`w-full p-3.5 pl-12 rounded-xl shadow-lg focus:outline-none focus:ring-2 ${
                    currentTheme === 'dark' 
                      ? `bg-[#2c2c2c] border-none text-white focus:ring-2`
                      : `bg-white border border-gray-300 text-gray-800 focus:ring-2`
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
            {isAnyPunishmentEnabled ? (
              <PunishmentTable 
                punishments={paginatedPunishments} 
                activeType={activeType} 
                serverColor={serverConfig.serverColor}
                currentTheme={currentTheme}
              />
            ) : (
              <div className={`p-10 rounded-xl text-center border border-dashed ${currentTheme === 'dark' ? 'border-gray-700 bg-[#242424]' : 'border-gray-300 bg-white'}`}>
                <h3 className="text-xl font-semibold mb-2">Seems like a config issue :/</h3>
                <p className="text-gray-500">No punishment types are enabled in the server configuration.</p>
              </div>
            )}
          </div>
          
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
            currentTheme={currentTheme}
          />

          {/* Render error/loading content within the main area */}
          {(error || isLoading) && renderContent()} 
        </main>
      </div>

      <FilterModal 
        isOpen={isFilterModalOpen} 
        onClose={() => setIsFilterModalOpen(false)} 
        onApply={handleFilterApply}
        serverColor={serverConfig.serverColor}
        currentTheme={currentTheme}
      />

      {serverConfig.isSecured && user && isAuthenticated &&
        <NewPunishmentModal 
          isOpen={isNewPunishmentModalOpen}
          onClose={handleCloseNewPunishmentModal}
          executorName={user.username}
          currentTheme={currentTheme}
        />
      }
    </div>
  );
};

export default PunishmentsPage;