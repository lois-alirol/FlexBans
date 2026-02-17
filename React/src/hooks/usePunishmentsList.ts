import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { PunishmentType } from '../types/punishments';
import { useServerConfig } from './useServerConfig';
import { useAuth } from './useAuth';
import { usePunishmentsData } from './usePunishmentsData';

interface PunishmentSetting {
    enabled: boolean;
    maxPerPage: number;
}

interface FilterOptions {
    executor: string;
    status: 'active' | 'expired' | 'removed' | '';
    dateFilterType: 'none' | 'exact' | 'before' | 'after';
    dateValue: string;
}

const scrollToTop = () => {
    window.scrollTo({
        top: 0,
        behavior: 'smooth',
    });
};

export const usePunishmentList = () => {
    const { serverConfig } = useServerConfig();
    const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const [currentPage, setCurrentPage] = useState(1);
    const [searchTerm, setSearchTerm] = useState('');
    const [activeType, setActiveType] = useState<PunishmentType | null>(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [filters, setFilters] = useState<FilterOptions>({
        executor: '',
        status: '',
        dateFilterType: 'none',
        dateValue: '',
    });

    const { punishmentsData, isLoading, error, pagination } = usePunishmentsData(undefined, currentPage, activeType);
    const { recentPunishments, globalCounts } = punishmentsData;

    useEffect(() => {
        const navState = location.state as { activeType?: PunishmentType | null } | null;
        if (navState && 'activeType' in navState) {
            setActiveType(navState.activeType ?? null);
            navigate('.', { replace: true, state: {} });
        }
    }, [location.state, navigate]);

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

    const handleApplyFilters = useCallback((newFilters: FilterOptions) => {
        setFilters(newFilters);
        setCurrentPage(1);
    }, []);

    useEffect(() => {
        if (currentPage >= 1) {
            scrollToTop();
        }
    }, [currentPage]);

    useEffect(() => {
        setCurrentPage(1);
    }, [activeType, searchTerm, filters]);

// inside usePunishmentList.ts

    useEffect(() => {
        // 1. Guard: Wait until the server config is actually loaded
        if (!serverConfig || serverConfig.serverName === 'Loading...') {
            return;
        }

        const punishmentSettings = Object.entries(serverConfig.punishments) as [string, PunishmentSetting][];
        const isAnyEnabled = punishmentSettings.some(([_, p]) => p.enabled);

        // 2. If nothing is enabled, we can't set a type
        if (!isAnyEnabled) {
            if (activeType !== null) setActiveType(null);
            return;
        }

        // 3. ONLY set a default if activeType is currently null/undefined
        if (!activeType) {
            const typePriority = ['bans', 'mutes', 'kicks', 'warnings'];
            const firstEnabledKey = typePriority.find(key =>
                serverConfig.punishments[key as keyof typeof serverConfig.punishments]?.enabled
            );

            if (firstEnabledKey) {
                const singular = firstEnabledKey.endsWith('s') ? firstEnabledKey.slice(0, -1) : firstEnabledKey;
                setActiveType(singular.toUpperCase() as PunishmentType);
            }
        }
    }, [serverConfig, activeType]); // Removed setActiveType to avoid unnecessary re-runs

    // Vérifier si des filtres sont actifs (hors recherche de base)
    const hasActiveFilters = filters.executor || filters.status || filters.dateFilterType !== 'none';

    const { paginatedPunishments, totalPages } = useMemo(() => {
        const serverTotalPages = pagination?.totalPages ?? 1;

        if (isAuthLoading || serverConfig.serverName === 'Loading...') {
            return { paginatedPunishments: [], totalPages: serverTotalPages };
        }
        if (isLoading) {
            return { paginatedPunishments: [], totalPages: serverTotalPages };
        }

        let punishments = recentPunishments;

        // Si aucun filtre n'est actif, utiliser la pagination serveur
        if (!searchTerm && !hasActiveFilters) {
            return { paginatedPunishments: punishments, totalPages: serverTotalPages };
        }

        // Sinon, appliquer les filtres côté client
        // Filtre par recherche de joueur
        if (searchTerm) {
            punishments = punishments.filter(p =>
                p.player.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        // Filtre par executor
        if (filters.executor) {
            punishments = punishments.filter(p =>
                p.moderator?.toLowerCase().includes(filters.executor.toLowerCase())
            );
        }

        // Filtre par status
        if (filters.status) {
            punishments = punishments.filter(p => {
                const punishmentStatus = (p.status || '').toLowerCase();
                return punishmentStatus === filters.status.toLowerCase();
            });
        }

        // Filtre par date
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

        // Pour les filtres côté client, pas de pagination (montrer tous les résultats filtrés)
        // ou implémenter une pagination client si trop de résultats
        return {
            paginatedPunishments: punishments,
            totalPages: 1 // Une seule page pour les résultats filtrés
        };
    }, [searchTerm, filters, hasActiveFilters, serverConfig, recentPunishments, isLoading, isAuthLoading, pagination]);

    useEffect(() => {
        if (isLoading || isAuthLoading || serverConfig.serverName === 'Loading...') return;

        let newPage = currentPage;

        if (totalPages > 0 && currentPage > totalPages) {
            newPage = totalPages;
        } else if (currentPage < 1) {
            newPage = 1;
        }

        if (newPage !== currentPage) {
            setCurrentPage(newPage);
        }
    }, [currentPage, totalPages, isLoading, isAuthLoading, serverConfig.serverName]);

    return {
        currentPage,
        searchTerm,
        activeType,
        isSidebarOpen,
        filters,

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
    };
};

// Fonction utilitaire pour parser les dates du format punishment
function parsePunishmentDate(dateStr: string): Date | null {
    if (!dateStr) return null;

    // Essayer différents formats de date
    // Format ISO: 2024-01-15T10:30:00
    let date = new Date(dateStr);
    if (!isNaN(date.getTime())) {
        return date;
    }

    // Format personnalisé: "15/01/2024 10:30:00" ou similaire
    const parts = dateStr.match(/(\d{1,2})\/(\d{1,2})\/(\d{4})/);
    if (parts) {
        const [, day, month, year] = parts;
        date = new Date(`${year}-${month}-${day}`);
        if (!isNaN(date.getTime())) {
            return date;
        }
    }

    // Format timestamp
    const timestamp = parseInt(dateStr);
    if (!isNaN(timestamp)) {
        return new Date(timestamp);
    }

    return null;
}