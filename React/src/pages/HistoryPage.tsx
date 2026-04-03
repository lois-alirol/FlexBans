import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { FaChartBar, FaUserCheck, FaUserTimes, FaShieldAlt } from 'react-icons/fa';
import { useLocation, useParams } from 'react-router-dom';

import type { Punishment } from '@/types/punishments';
import type { PlayerStats, StaffStats } from '@/types/punishments';

import { usePunishmentsData } from '@hooks/usePunishmentsData';
import { useServerConfig } from '@hooks/useServerConfig';
import { usePlayerInfo } from '@hooks/usePlayerInfo';

import HistoryPageHeader from '@components/history/HistoryPageHeader';
import ProfileHeader from '@components/history/ProfileHeader';
import StatsCards from '@components/history/StatsCards';
import PunishmentBreakdown from '@components/history/PunishmentBreakdown';
import SearchBar from '@components/punishments/list/SearchBar';
import DetailedLogsSection from '@components/history/DetailedLogsSection';
import LoadingState from '@components/history/LoadingState';
import EmptyProfileState from '@components/history/EmptyProfileState';
import FilterModal from '@components/common/modals/FilterModal';
import {useTitle} from "@hooks/useTitle.ts";
import {useTranslation} from "react-i18next";

type HistoryContext = 'player' | 'moderator';

interface FilterOptions {
    executor: string;
    status: 'active' | 'expired' | 'removed' | '';
    dateFilterType: 'none' | 'exact' | 'before' | 'after';
    dateValue: string;
}

const HistoryPage: React.FC = () => {
    const { serverConfig } = useServerConfig();
    const { t } = useTranslation();

    const location = useLocation();
    const params = useParams();

    const isPlayerContext = location.pathname.startsWith('/player/');
    const contextType: HistoryContext = isPlayerContext ? 'player' : 'moderator';

    const itemsPerPage = isPlayerContext && serverConfig
        ? serverConfig && serverConfig.histories?.playerMaxPerPage || 20
        : serverConfig && serverConfig.histories?.moderatorMaxPerPage || 20;

    const associatedUser = (isPlayerContext ? params.playerName : params.moderatorName) || '';

    const [currentPage, setCurrentPage] = useState(1);
    const [searchTerm, setSearchTerm] = useState('');
    const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
    const [isDownloading, setIsDownloading] = useState(false);
    const [filters, setFilters] = useState<FilterOptions>({
        executor: '',
        status: '',
        dateFilterType: 'none',
        dateValue: '',
    });

    const [allUserPunishments, setAllUserPunishments] = useState<Punishment[]>([]);
    const [isLoadingAll, setIsLoadingAll] = useState(true);

    const { playerInfo: associatedUserInfo } = usePlayerInfo(associatedUser);

    useEffect(() => {
        const loadAllPunishments = async () => {
            if (!associatedUser) return;

            setIsLoadingAll(true);
            const allPunishments: Punishment[] = [];
            let page = 1;
            let hasMore = true;

            try {
                while (hasMore) {
                    const response = await fetch(
                        `${import.meta.env.VITE_APP_API_URL}/punishments?page=${page}`,
                        { credentials: 'include' }
                    );

                    if (!response.ok) break;

                    const data = await response.json();
                    if (data.code !== 200 || !data.data?.items) break;

                    const items = data.data.items;
                    const userItems = items.filter((p: Punishment) =>
                        isPlayerContext ? p.player === associatedUser : p.moderator === associatedUser
                    );

                    allPunishments.push(...userItems);

                    if (page >= data.data.totalPages) {
                        hasMore = false;
                    } else {
                        page++;
                    }
                }

                setAllUserPunishments(allPunishments);
            } catch (error) {
                console.error('Error loading all punishments:', error);
            } finally {
                setIsLoadingAll(false);
            }
        };

        void loadAllPunishments();
    }, [associatedUser, isPlayerContext]);

    const { punishmentsData, isLoading: isStatsLoading } = usePunishmentsData();
    const { userStats } = punishmentsData;

    const punishmentStats = useMemo(() => {
        const stats = associatedUser ? userStats[associatedUser] : undefined;
        if (stats) return stats;
        return isPlayerContext
            ? ({ received: 0, active: 0, removed: 0, expired: 0 } as PlayerStats)
            : ({ sent: 0, active: 0, removed: 0, bansSent: 0 } as StaffStats);
    }, [associatedUser, isPlayerContext, userStats]);

    const chips = useMemo(() => {
        if ('received' in punishmentStats) {
            const p = punishmentStats as PlayerStats;
            return [
                { title: t("history.stats.player.total"), value: p.received ?? 0, icon: <FaChartBar />, accent: 'text-indigo-400 bg-indigo-500/20' },
                { title: t("history.stats.player.active"), value: p.active ?? 0, icon: <FaUserTimes />, accent: 'text-red-400 bg-red-500/20' },
                { title: t("history.stats.player.removed"), value: p.removed ?? 0, icon: <FaUserCheck />, accent: 'text-green-400 bg-green-500/20' },
                { title: t("history.stats.player.expired"), value: p.expired ?? 0, icon: <FaChartBar />, accent: 'text-yellow-400 bg-yellow-500/20' },
            ];
        }
        const s = punishmentStats as StaffStats;
        return [
            { title: t("history.stats.staff.total"), value: s.sent ?? 0, icon: <FaChartBar />, accent: 'text-indigo-400 bg-indigo-500/20' },
            { title: t("history.stats.staff.bans"), value: s.bansSent ?? 0, icon: <FaUserTimes />, accent: 'text-red-400 bg-red-500/20' },
            { title: t("history.stats.staff.active"), value: s.active ?? 0, icon: <FaShieldAlt />, accent: 'text-green-400 bg-green-500/20' },
            { title: t("history.stats.staff.revoked"), value: s.removed ?? 0, icon: <FaChartBar />, accent: 'text-yellow-400 bg-yellow-500/20' },
        ];
    }, [punishmentStats]);

    const distribution = useMemo(() => {
        const counts = { bans: 0, mutes: 0, kicks: 0, warnings: 0 };
        allUserPunishments.forEach(p => {
            const type = p.type?.toLowerCase() || '';
            if (type.includes('ban')) counts.bans++;
            else if (type.includes('mute')) counts.mutes++;
            else if (type.includes('kick')) counts.kicks++;
            else if (type.includes('warn')) counts.warnings++;
        });
        return [
            { label: 'Bans', count: counts.bans, color: 'bg-red-500' },
            { label: 'Mutes', count: counts.mutes, color: 'bg-orange-500' },
            { label: 'Kicks', count: counts.kicks, color: 'bg-yellow-500' },
            { label: 'Warnings', count: counts.warnings, color: 'bg-blue-500' },
        ];
    }, [allUserPunishments]);

    const parsePunishmentDate = (dateStr: string): Date | null => {
        if (!dateStr) return null;

        let date = new Date(dateStr);
        if (!isNaN(date.getTime())) return date;

        const parts = dateStr.match(/(\d{1,2})\/(\d{1,2})\/(\d{4})/);
        if (parts) {
            const [, day, month, year] = parts;
            date = new Date(`${year}-${month}-${day}`);
            if (!isNaN(date.getTime())) return date;
        }

        const timestamp = parseInt(dateStr);
        if (!isNaN(timestamp)) return new Date(timestamp);

        return null;
    };

    const { filteredPunishments, totalPages } = useMemo(() => {
        if (!associatedUser) return { filteredPunishments: [], totalPages: 0 };

        let punishments = [...allUserPunishments];

        if (searchTerm) {
            const searchField = isPlayerContext ? 'moderator' : 'player';
            punishments = punishments.filter(p => {
                const valueToSearch = (p[searchField as keyof Punishment] as string) || '';
                return valueToSearch.toLowerCase().includes(searchTerm.toLowerCase());
            });
        }

        if (filters.executor && !isPlayerContext) {
            punishments = punishments.filter(p =>
                p.moderator?.toLowerCase().includes(filters.executor.toLowerCase())
            );
        }

        if (filters.status) {
            punishments = punishments.filter(p => {
                const punishmentStatus = (p.status || '').toLowerCase();
                return punishmentStatus === filters.status.toLowerCase();
            });
        }

        if (filters.dateFilterType !== 'none' && filters.dateValue) {
            const filterDate = new Date(filters.dateValue);
            filterDate.setHours(0, 0, 0, 0);

            punishments = punishments.filter(p => {
                if (!p.date) return false;

                const punishmentDate = parsePunishmentDate(p.date);
                if (!punishmentDate) return false;

                punishmentDate.setHours(0, 0, 0, 0);

                switch (filters.dateFilterType) {
                    case 'exact':
                        return punishmentDate.getTime() === filterDate.getTime();
                    case 'before':
                        return punishmentDate.getTime() < filterDate.getTime();
                    case 'after':
                        return punishmentDate.getTime() > filterDate.getTime();
                    default:
                        return true;
                }
            });
        }

        const totalPages = Math.max(1, Math.ceil(punishments.length / itemsPerPage));
        const startIndex = (currentPage - 1) * itemsPerPage;
        const paginatedItems = punishments.slice(startIndex, startIndex + itemsPerPage);

        return { filteredPunishments: paginatedItems, totalPages };
    }, [allUserPunishments, searchTerm, filters, currentPage, isPlayerContext, associatedUser, itemsPerPage]);

    const handlePageChange = useCallback((page: number) => {
        if (page > 0 && page <= totalPages) setCurrentPage(page);
    }, [totalPages]);

    const handleFilterApply = useCallback((newFilters: FilterOptions) => {
        setFilters(newFilters);
        setCurrentPage(1);
        setIsFilterModalOpen(false);
    }, []);

    const handleDownloadSkin = async () => {
        if (!associatedUser) return;
        setIsDownloading(true);
        const skinUrl = `https://mc-heads.net/skin/${encodeURIComponent(associatedUser)}`;
        try {
            const response = await fetch(skinUrl);
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${associatedUser}_skin.png`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (error) {
            window.open(skinUrl, '_blank');
        } finally {
            setIsDownloading(false);
        }
    };

    useEffect(() => {
        setCurrentPage(1);
    }, [searchTerm, filters]);

    useEffect(() => {
        if (currentPage > totalPages && totalPages > 0) {
            setCurrentPage(totalPages);
        }
    }, [currentPage, totalPages]);

    const bgColor = 'bg-background';
    const textColor = 'text-text-primary';
    const cardBg = 'bg-surface';
    const borderColor = 'border-surface';

    if (!associatedUser) return null;

    const isLoading = isLoadingAll || isStatsLoading;
    const isConsole = associatedUser.toLowerCase() === 'console';

    const contextBadgeStyle = isPlayerContext
        ? 'bg-blue-500/10 text-blue-500'
        : (isConsole ? 'bg-gray-500/10 text-gray-500' : 'bg-green-800/60 text-green-400');

    useTitle(
        isConsole
            ? `Console (${t("history.badges.short.console")})`
            : (isPlayerContext ? associatedUser : `${associatedUser} (${t("history.badges.short.staff")})`)
    );

    if (isLoading) {
        return (
            <LoadingState
                bgColor={bgColor}
                textColor={textColor}
                cardBg={cardBg}
                borderColor={borderColor}
            />
        );
    }

    if (isPlayerContext && isConsole) {
        return (
            <EmptyProfileState type="console" />
        );
    }

    if (contextType === 'moderator' && allUserPunishments.length === 0) {
        return (
            <EmptyProfileState type="no-punishments" />
        );
    }

    return (
        <div className={`min-h-screen ${bgColor} ${textColor} pb-20`}>
            <HistoryPageHeader
                isConsole={isConsole}
                isPlayerContext={isPlayerContext}
                bgColor={bgColor}
                borderColor={borderColor}
                contextBadgeStyle={contextBadgeStyle}
            />

            <div className="max-w-6xl mx-auto px-4 mt-10">
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-10">
                    <ProfileHeader
                        username={associatedUser}
                        prefix={associatedUserInfo?.prefix}
                        isPlayerContext={isPlayerContext}
                        isDownloading={isDownloading}
                        cardBg={cardBg}
                        onDownloadSkin={handleDownloadSkin}
                    />

                    <div className="lg:col-span-8 flex flex-col gap-4">
                        <StatsCards chips={chips} />

                        <PunishmentBreakdown
                            distribution={distribution}
                            totalPunishments={allUserPunishments.length}
                            cardBg={cardBg}
                        />

                        <SearchBar
                            searchTerm={searchTerm}
                            onSearchChange={setSearchTerm}
                            onFilterClick={() => setIsFilterModalOpen(true)}
                            placeholder={t("history.search-placeholder")}
                        />
                    </div>
                </div>

                <DetailedLogsSection
                    filteredPunishments={filteredPunishments}
                    totalPunishments={allUserPunishments.length}
                    currentPage={currentPage}
                    totalPages={totalPages}
                    contextType={contextType}
                    onPageChange={handlePageChange}
                />
            </div>

            <FilterModal
                isOpen={isFilterModalOpen}
                onClose={() => setIsFilterModalOpen(false)}
                onApply={handleFilterApply}
            />
        </div>
    );
};

export default HistoryPage;