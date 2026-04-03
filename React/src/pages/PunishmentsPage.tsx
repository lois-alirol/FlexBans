import React, {useState} from 'react';

import {useServerConfig} from '@hooks/useServerConfig';
import {useAuth} from '@hooks/useAuth';
import {usePunishmentList} from '@hooks/usePunishmentsList';
import {useTitle} from "@hooks/useTitle";

import EmptyState from '@components/punishments/list/EmptyState';
import Sidebar from '@components/common/sidebar/Sidebar';
import PageHeader from '@components/punishments/PageHeader';
import SearchBar from '@components/punishments/list/SearchBar';
import PunishmentTable from '@components/punishments/list/PunishmentTable';
import Pagination from '@components/common/Pagination';
import NewPunishmentModal from '@components/common/modals/NewPunishmentModal';
import FilterModal from '@components/common/modals/FilterModal';
import Notification from "@components/common/Notification";

import {useTranslation} from "react-i18next";
import Error500 from "@pages/errors/Error500.tsx";

interface PunishmentSetting {
    enabled: boolean;
    maxPerPage: number;
}

const PunishmentsPage: React.FC = () => {
    const { serverConfig, isLoading: isConfigLoading } = useServerConfig();
    const { isAuthenticated, user } = useAuth();
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

    useTitle(
        `${(activeType
                ? activeType[0].toUpperCase() + activeType.slice(1).toLowerCase()
                : ""
        )}s`
    );

    const { t } = useTranslation();

    const [sidebarWidth, setSidebarWidth] = useState(280);
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);

    const [isNewPunishmentModalOpen, setIsNewPunishmentModalOpen] = useState(false);
    const [notification, setNotification] = useState<{
        visible: boolean;
        message: string;
        type: 'success' | 'error' | 'info';
    }>({
        visible: false,
        message: '',
        type: 'info',
    });

    const typeDisplay = activeType ? `${activeType}S` : '';
    const searchPlaceholder = t("home.search-placeholder");

    const isAnyPunishmentEnabled = serverConfig && (Object.values(serverConfig.punishments) as PunishmentSetting[]).some(p => p.enabled);

    const triggerNotif = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
        setNotification({ visible: true, message, type });
    };

    const handleFilterApply = (filters: any) => {
        handleApplyFilters(filters);
        setIsFilterModalOpen(false);
        triggerNotif(t("filter-modal.apply-notification"), 'info');
    };

    const handleNewPunishmentSuccess = () => {
        setIsNewPunishmentModalOpen(false);
        triggerNotif(t("create-modal.success.message"), 'success');
    };

    const handleRevokeSuccess = () => {
        triggerNotif(t("revoke-modal.success.message"), 'success');
    };

    const handleEditSuccess = () => {
        triggerNotif(t("edition-modal.success.message"), 'success');
    };

    const handlePreferencesApply = () => {
        triggerNotif(t("preferences-modal.success.message"), 'success');
    };

    if (isAuthLoading || isConfigLoading) {
        return (
            <EmptyState
                type="checking-access"
            />
        );
    }

    if (error) {
        return (
            <Error500 stackTrace={error}/>
        );
    }

    if (isLoading) {
        return (
            <EmptyState
                type="loading"
            />
        );
    }

    return (
        <div className={`flex min-h-screen bg-background text-text-primary`}>
            <Notification
                message={notification.message}
                type={notification.type}
                visible={notification.visible}
                onClose={() => setNotification(prev => ({ ...prev, visible: false }))}
            />

            <Sidebar
                counts={globalCounts}
                activeType={activeType}
                setActiveType={setActiveType}
                isSidebarOpen={isSidebarOpen}
                setIsSidebarOpen={setIsSidebarOpen}
                onNewPunishmentClick={() => setIsNewPunishmentModalOpen(true)}
                sidebarWidth={sidebarWidth}
                setSidebarWidth={setSidebarWidth}
            />

            <div
                className="flex flex-col grow min-w-0 pt-16 lg:pt-0"
                style={{
                    marginLeft: window.innerWidth >= 1024 ? `${sidebarWidth}px` : '0px',
                    transition: 'margin-left 300ms cubic-bezier(0.4, 0, 0.2, 1)'
                }}
            >
                <main className="p-4 sm:p-6 lg:p-8 flex-1 min-w-0">
                    <PageHeader
                        serverName={serverConfig ? serverConfig.serverName : ''}
                        typeDisplay={typeDisplay}
                        isAuthenticated={isAuthenticated}
                        isSecured={serverConfig ? serverConfig.isSecured : false}
                        onPreferencesApply={handlePreferencesApply}
                    />

                    <SearchBar
                        searchTerm={searchTerm}
                        onSearchChange={setSearchTerm}
                        onFilterClick={() => setIsFilterModalOpen(true)}
                        placeholder={searchPlaceholder}
                    />

                    <div className="overflow-x-auto rounded-lg">
                        {isAnyPunishmentEnabled ? (
                            <PunishmentTable
                                punishments={paginatedPunishments}
                                activeType={activeType}
                                onRevokeSuccess={handleRevokeSuccess}
                                onEditSuccess={handleEditSuccess}
                            />
                        ) : (
                            <EmptyState
                                type="config-error"
                            />
                        )}
                    </div>

                    <Pagination
                        currentPage={currentPage}
                        totalPages={totalPages}
                        onPageChange={handlePageChange}
                    />
                </main>
            </div>

            <FilterModal
                isOpen={isFilterModalOpen}
                onClose={() => setIsFilterModalOpen(false)}
                onApply={handleFilterApply}
            />

            {serverConfig && !isConfigLoading && serverConfig.isSecured && user && isAuthenticated && (
                <NewPunishmentModal
                    isOpen={isNewPunishmentModalOpen}
                    onClose={() => setIsNewPunishmentModalOpen(false)}
                    onSuccess={handleNewPunishmentSuccess}
                    executorName={user.username}
                />
            )}
        </div>
    );
};

export default PunishmentsPage;