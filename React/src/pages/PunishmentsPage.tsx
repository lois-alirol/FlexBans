import {useServerConfig} from "../hooks/useServerConfig.ts";
import {useAuth} from "../hooks/useAuth.ts";
import {useTheme} from "../hooks/useTheme.ts";
import {usePunishmentList} from "../hooks/usePunishmentsList.ts";
import React, {useState} from "react";
import EmptyState from "../components/punishments/list/EmptyState.tsx";
import Sidebar from "../components/common/sidebar/Sidebar.tsx";
import MobileHeader from "../components/common/sidebar/MobileHeader.tsx";
import PageHeader from "../components/punishments/PageHeader.tsx";
import SearchBar from "../components/punishments/list/SearchBar.tsx";
import PunishmentTable from "../components/punishments/list/PunishmentTable.tsx";
import Pagination from "../components/common/Pagination.tsx";
import NewPunishmentModal from "../components/common/modals/NewPunishmentModal.tsx";
import FilterModal from "../components/common/modals/FilterModal.tsx";

interface PunishmentSetting {
  enabled: boolean;
  maxPerPage: number;
}

const PunishmentsPage: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated, user } = useAuth();
  const { isDarkMode } = useTheme();
  const {
    currentPage,
    searchTerm,
    activeType,
    isSidebarOpen,
    paginatedPunishments,
    globalCounts,
    totalPages,
    isLoading,
    error,
    isAuthLoading,
    setSearchTerm,
    setActiveType,
    setIsSidebarOpen,
    handlePageChange,
    handleApplyFilters,
  } = usePunishmentList();

  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [isNewPunishmentModalOpen, setIsNewPunishmentModalOpen] = useState(false);

  const currentTheme = isDarkMode ? 'dark' : 'light';
  const typeDisplay = activeType ? `${activeType}S` : 'All Punishments';
  const searchPlaceholder = activeType ? `Search Player for ${activeType}...` : 'Search Player...';
  const titleActiveType = activeType || 'Overview';

  const isAnyPunishmentEnabled = (Object.values(serverConfig.punishments) as PunishmentSetting[]).some(p => p.enabled);

  const handleFilterApply = (filters: any) => {
    handleApplyFilters(filters);
    setIsFilterModalOpen(false);
  };

  const renderMainContent = () => {
    if (isAuthLoading || serverConfig.serverName === 'Loading...') {
      return (
          <EmptyState
              type="checking-access"
              currentTheme={currentTheme}
              serverColor={serverConfig.serverColor}
          />
      );
    }

    if (error) {
      return (
          <EmptyState
              type="error"
              currentTheme={currentTheme}
              errorMessage={error}
          />
      );
    }

    if (isLoading) {
      return (
          <EmptyState
              type="loading"
              currentTheme={currentTheme}
              serverColor={serverConfig.serverColor}
          />
      );
    }

    return null;
  };

  if (isAuthLoading || (serverConfig.isSecured && !isAuthenticated)) {
    return (
        <div className={`flex min-h-screen items-center justify-center ${currentTheme === 'dark' ? 'bg-[#1c1c1c] text-[#e0e0e0]' : 'bg-[#f4f4f4] text-[#333333]'}`}>
          {renderMainContent()}
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
            onNewPunishmentClick={() => setIsNewPunishmentModalOpen(true)}
        />

        <div className={`flex flex-col grow transition-all duration-300 ${isSidebarOpen ? 'lg:ml-64' : 'lg:ml-20'}`}>
          <MobileHeader
              onMenuClick={() => setIsSidebarOpen(true)}
              serverLogo={serverConfig.serverLogo}
              currentTheme={currentTheme}
          />

          <main className="p-4 sm:p-6 lg:p-8 flex-1 min-w-0">
            <PageHeader
                serverName={serverConfig.serverName}
                typeDisplay={typeDisplay}
                isAuthenticated={isAuthenticated}
                isSecured={serverConfig.isSecured}
                currentTheme={currentTheme}
            />

            <SearchBar
                searchTerm={searchTerm}
                onSearchChange={setSearchTerm}
                onFilterClick={() => setIsFilterModalOpen(true)}
                placeholder={searchPlaceholder}
                currentTheme={currentTheme}
                serverColor={serverConfig.serverColor}
            />

            <div className="overflow-x-auto shadow-xl rounded-lg">
              {isAnyPunishmentEnabled ? (
                  <PunishmentTable
                      punishments={paginatedPunishments}
                      activeType={activeType}
                      serverColor={serverConfig.serverColor}
                      currentTheme={currentTheme}
                  />
              ) : (
                  <EmptyState
                      type="config-error"
                      currentTheme={currentTheme}
                  />
              )}
            </div>

            <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={handlePageChange}
                currentTheme={currentTheme}
            />

            {(error || isLoading) && renderMainContent()}
          </main>
        </div>

        <FilterModal
            isOpen={isFilterModalOpen}
            onClose={() => setIsFilterModalOpen(false)}
            onApply={handleFilterApply}
            serverColor={serverConfig.serverColor}
            currentTheme={currentTheme}
        />

        {serverConfig.isSecured && user && isAuthenticated && (
            <NewPunishmentModal
                isOpen={isNewPunishmentModalOpen}
                onClose={() => setIsNewPunishmentModalOpen(false)}
                executorName={user.username}
                currentTheme={currentTheme}
            />
        )}
      </div>
  );
};

export default PunishmentsPage;