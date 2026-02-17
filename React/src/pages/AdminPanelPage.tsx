import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { FaUsersCog, FaCogs, FaChartBar, FaShieldAlt, FaTerminal } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import MyAccount from '../components/common/MyAccount';
import { useAuth } from '../hooks/useAuth';
import { useServerConfig } from '../hooks/useServerConfig';
import { useTheme } from '../hooks/useTheme';
import { usePunishmentsData } from '../hooks/usePunishmentsData';
import LogsModal from '../components/admin/LogsModal';

import MetricCard from '../components/admin/MetricCard';
import CardSection from '../components/admin/CardSection';
import UserManagement from '../components/admin/UserManagement';
import BrandingMeta from '../components/admin/BrandingMeta';
import PunishmentSettings from '../components/admin/PunishmentSettings';
import DetailHistorySettings from '../components/admin/DetailHistorySettings';
import Error403 from './errors/Error403';

import type { BrandingForm, Theme as UiTheme } from '../types/admin';
import type { ServerConfig } from '../types/config';
import type { PunishmentType } from '../types/punishments';
import NewPunishmentModal from "../components/common/modals/NewPunishmentModal.tsx";
import Sidebar from "../components/common/sidebar/Sidebar.tsx";
import { useUsers } from "../hooks/useUsers.ts";

type PunishmentKey = keyof ServerConfig['punishments'];

const AdminPanelPage: React.FC = () => {
    const { serverConfig } = useServerConfig();
    const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();
    const { currentTheme } = useTheme();
    const navigate = useNavigate();

    const { isLoading: isUsersLoading } = useUsers();

    const { punishmentsData } = usePunishmentsData();
    const { globalCounts } = punishmentsData;

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [isNewPunishmentModalOpen, setIsNewPunishmentModalOpen] = useState(false);
    const [isLogsModalOpen, setIsLogsModalOpen] = useState(false);

    const handleOpenNewPunishmentModal = useCallback(() => setIsNewPunishmentModalOpen(true), []);
    const handleCloseNewPunishmentModal = useCallback(() => setIsNewPunishmentModalOpen(false), []);

    const isConfigLoading = serverConfig.serverName === 'Loading...';
    const [form, setForm] = useState<null | {
        branding: BrandingForm;
        features: { allowExecution: boolean; allowRevocation: boolean };
        punishments: ServerConfig['punishments'];
        details: {
            moderator: { enabled: boolean; maxPerPage: number };
            player: { enabled: boolean; maxPerPage: number };
            punishment: { enabled: boolean; revokeButton: boolean };
        };
    }>(null);

    useEffect(() => {
        if (isConfigLoading) return;

        setForm({
            branding: {
                serverName: serverConfig.serverName || '',
                primaryColor: serverConfig.serverColor || '#4f46e5',
                secondaryColor: serverConfig.serverColorHover || '#6366f1',
                logo: serverConfig.serverLogo || '',
                favicon: serverConfig.serverFavicon || '',
                description: serverConfig.serverDescription || '',
            },
            features: {
                allowExecution: serverConfig.punishmentExecution ?? true,
                allowRevocation: serverConfig.punishmentRevocation ?? true,
            },
            punishments: {
                bans: {
                    enabled: serverConfig.punishments?.bans?.enabled ?? true,
                    maxPerPage: serverConfig.punishments?.bans?.maxPerPage ?? 20,
                },
                mutes: {
                    enabled: serverConfig.punishments?.mutes?.enabled ?? true,
                    maxPerPage: serverConfig.punishments?.mutes?.maxPerPage ?? 20,
                },
                warnings: {
                    enabled: serverConfig.punishments?.warnings?.enabled ?? true,
                    maxPerPage: serverConfig.punishments?.warnings?.maxPerPage ?? 20,
                },
                kicks: {
                    enabled: serverConfig.punishments?.kicks?.enabled ?? true,
                    maxPerPage: serverConfig.punishments?.kicks?.maxPerPage ?? 20,
                },
            },
            details: {
                moderator: {
                    enabled: true,
                    maxPerPage: serverConfig.histories?.moderatorMaxPerPage ?? 20,
                },
                player: {
                    enabled: true,
                    maxPerPage: serverConfig.histories?.playerMaxPerPage ?? 20,
                },
                punishment: {
                    enabled: true,
                    revokeButton: serverConfig.punishmentRevocation ?? true,
                },
            },
        });
    }, [isConfigLoading, serverConfig]);

    const accent = serverConfig.serverColor || '#4f46e5';
    const theme: UiTheme = currentTheme;

    const isAdmin = useMemo(() => !!user?.permissions?.includes('flexbans.web.admin'), [user]);

    useEffect(() => {
        if (!isAuthLoading && serverConfig.isSecured && !isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, isAuthLoading, serverConfig.isSecured, navigate]);

    const updateBranding = useCallback((key: keyof BrandingForm, value: string) => {
        setForm((prev) => (prev ? { ...prev, branding: { ...prev.branding, [key]: value } } : prev));
    }, []);

    const updateFeature = useCallback((key: keyof NonNullable<typeof form>['features'], value: boolean) => {
        setForm((prev) => (prev ? { ...prev, features: { ...prev.features, [key]: value } } : prev));
    }, []);

    const updatePunishment = useCallback(
        (key: PunishmentKey, patch: Partial<ServerConfig['punishments'][PunishmentKey]>) => {
            setForm((prev) =>
                prev
                    ? { ...prev, punishments: { ...prev.punishments, [key]: { ...prev.punishments[key], ...patch } } }
                    : prev
            );
        },
        []
    );

    const updateDetails = useCallback(
        (key: keyof NonNullable<typeof form>['details'], patch: Partial<NonNullable<typeof form>['details'][typeof key]>) => {
            setForm((prev) =>
                prev
                    ? { ...prev, details: { ...prev.details, [key]: { ...prev.details[key], ...patch } } }
                    : prev
            );
        },
        []
    );

    const handleSidebarSetActiveType = useCallback(
        (type: PunishmentType | null) => {
            if (!type) return;

            navigate('/', { state: { activeType: type } });
        },
        [navigate]
    );

    const mockLogs = useMemo(
        () => [
            '[2026-01-11 12:00:00] INFO  server: Admin panel loaded.',
            '[2026-01-11 12:00:03] INFO  auth: User Neocle authenticated.',
            '[2026-01-11 12:00:05] WARN  api: Slow response from /punishments (420ms).',
            '[2026-01-11 12:00:08] INFO  ui: Sidebar toggled open.',
            '[2026-01-11 12:00:12] INFO  metrics: Refreshed dashboard metrics.',
            '[2026-01-11 12:00:15] ERROR api: Failed to sync logs (mock).',
            '[2026-01-11 12:00:20] INFO  ui: Logs modal opened.',
        ],
        []
    );

    if (isAuthLoading || isConfigLoading || isUsersLoading || !form) {
        return (
            <div className={`flex items-center justify-center min-h-screen ${theme === 'dark' ? 'bg-[#1c1c1c] text-gray-100' : 'bg-gray-50 text-gray-800'}`}>
                <div className={`flex items-center space-x-3 p-8 rounded-xl ${theme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white'} shadow-xl`}>
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2" style={{ borderColor: accent }} />
                    <p className="text-xl font-semibold">Loading configuration...</p>
                </div>
            </div>
        );
    }

    if (serverConfig.isSecured && (!isAuthenticated || !isAdmin)) {
        return <Error403 />;
    }

    return (
        <div className={`flex min-h-screen ${theme === 'dark' ? 'bg-[#1c1c1c] text-[#e0e0e0]' : 'bg-[#f4f4f4] text-[#333333]'}`}>
            <title>{serverConfig.serverName} - Admin Panel</title>

            <Sidebar
                serverConfig={serverConfig}
                counts={globalCounts}
                activeType={null}
                setActiveType={handleSidebarSetActiveType}
                isSidebarOpen={isSidebarOpen}
                setIsSidebarOpen={setIsSidebarOpen}
                onNewPunishmentClick={handleOpenNewPunishmentModal}
            />

            <div className={`flex flex-col grow transition-all duration-300 ${isSidebarOpen ? 'lg:ml-64' : 'lg:ml-20'}`}>
                <header className={`flex items-center justify-between p-4 ${theme === 'dark' ? 'bg-[#242424]' : 'bg-[#e4e4e4]'} lg:hidden shadow-md`}>
                    <button onClick={() => setIsSidebarOpen(true)} aria-label="Open menu" className="text-xl">
                        <FaUsersCog />
                    </button>
                    <img src={form.branding.logo || serverConfig.serverLogo} alt="Logo" className="h-6" />
                </header>

                <main className="p-4 sm:p-6 lg:p-8 space-y-8">
                    <div className="flex items-start justify-between">
                        <div>
                            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">Admin Panel</h1>
                            <p className="text-sm sm:text-base mt-2 text-gray-500 dark:text-gray-400">
                                Manage users, branding, punishments, and detail settings.
                            </p>
                        </div>
                        {serverConfig.isSecured && isAuthenticated && (
                            <div className="hidden lg:block mt-1">
                                <MyAccount currentTheme={theme} />
                            </div>
                        )}
                    </div>

                    {/* Metrics */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                        <MetricCard icon={<FaUsersCog />} label="Total Users" value="128" accent={accent} theme={theme} />
                        <MetricCard icon={<FaShieldAlt />} label="Admins" value="6" accent={accent} theme={theme} />
                        <MetricCard icon={<FaChartBar />} label="Active Sessions" value="42" accent={accent} theme={theme} />
                        <MetricCard icon={<FaCogs />} label="Pending Actions" value="3" accent={accent} theme={theme} />
                    </div>

                    {/* User Management */}
                    <CardSection title="User Management" accent={accent} theme={theme} description="Manage panel users, their permissions, and access.">
                        <UserManagement searchTerm={searchTerm} onSearch={setSearchTerm} accent={accent} theme={theme} />
                    </CardSection>

                    {/* Branding */}
                    <CardSection title="Branding & Meta" accent={accent} theme={theme} description="Control server name, colors, logo, favicon, and description.">
                        <BrandingMeta branding={form.branding} theme={theme} accent={accent} onChange={updateBranding} />
                    </CardSection>

                    {/* Punishment Settings */}
                    <CardSection title="Punishment Settings" accent={accent} theme={theme} description="Enable or disable categories and control pagination limits.">
                        <PunishmentSettings
                            punishments={form.punishments}
                            features={form.features}
                            theme={theme}
                            accent={accent}
                            onUpdatePunishment={updatePunishment}
                            onUpdateFeature={updateFeature}
                        />
                    </CardSection>

                    {/* Detail / History Settings */}
                    <CardSection title="Detail & History Settings" accent={accent} theme={theme} description="Control visibility and pagination for moderator/player detail pages.">
                        <DetailHistorySettings details={form.details} theme={theme} accent={accent} onUpdateDetails={updateDetails} />
                    </CardSection>

                    {/* Logs Card (separate, above actions) */}
                    <div
                        className={`rounded-2xl border shadow-md px-5 py-4 flex items-center justify-between gap-4 ${
                            theme === 'dark'
                                ? 'bg-[#1f1f1f] border-gray-700 text-gray-100 hover:border-gray-500'
                                : 'bg-white border-gray-200 text-gray-800 hover:border-gray-300'
                        } transition hover:-translate-y-0.5 active:translate-y-0`}
                        role="button"
                        tabIndex={0}
                        onClick={() => setIsLogsModalOpen(true)}
                        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setIsLogsModalOpen(true)}
                    >
                        <div className="flex items-center gap-3">
                            <div
                                className={`rounded-xl p-3 ${
                                    theme === 'dark' ? 'bg-[#2b2b2b] text-[#8ef0a3]' : 'bg-[#eef9f0] text-[#1f7a3d]'
                                }`}
                            >
                                <FaTerminal className="text-lg" />
                            </div>
                            <div className="text-left">
                                <p className="text-sm font-semibold">Check Logs</p>
                                <p className="text-xs text-gray-500 dark:text-gray-400">View recent system events</p>
                            </div>
                        </div>
                        <span
                            className="text-xs font-semibold px-2 py-1 rounded-full"
                            style={{ backgroundColor: `${accent}20`, color: accent }}
                        >
                            Open
                        </span>
                    </div>

                    {/* Actions */}
                    <div className="flex flex-wrap justify-end gap-3">
                        <button className={`px-4 py-2 rounded-xl text-sm font-semibold ${theme === 'dark' ? 'bg-[#2c2c2c] text-white' : 'bg-gray-200 text-gray-800'} shadow`}>
                            Cancel
                        </button>
                        <button
                            className="px-5 py-2 rounded-xl text-sm font-semibold text-white shadow-md hover:scale-[1.01] active:scale-[0.99] transition"
                            style={{ backgroundColor: accent }}
                            onClick={() => {
                                alert('Settings saved (mock). Wire to your API/mutation here.');
                            }}
                        >
                            Save Changes
                        </button>
                    </div>
                </main>
            </div>

            {serverConfig.isSecured && user && isAuthenticated && (
                <NewPunishmentModal
                    isOpen={isNewPunishmentModalOpen}
                    onClose={handleCloseNewPunishmentModal}
                    executorName={user.username}
                    currentTheme={theme}
                />
            )}

            <LogsModal
                isOpen={isLogsModalOpen}
                onClose={() => setIsLogsModalOpen(false)}
                accent={accent}
                theme={theme}
                logs={mockLogs}
            />
        </div>
    );
};

export default AdminPanelPage;