import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { FaUsersCog, FaCogs, FaChartBar, FaShieldAlt, FaTerminal } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

import type { BrandingForm } from '@/types/admin';
import type { ServerConfig } from '@/types/config';
import type { PunishmentType } from '@/types/punishments';

import { useAuth } from '@hooks/useAuth';
import { useServerConfig } from '@hooks/useServerConfig';
import { usePunishmentsData } from '@hooks/usePunishmentsData';
import { useUsers } from '@hooks/useUsers';
import { useTitle } from "@hooks/useTitle.ts";

import Error403 from '@pages/errors/Error403';

import LogsModal from '@components/common/modals/LogsModal';
import MyAccount from '@components/common/MyAccount';
import MetricCard from '@components/admin/MetricCard';
import CardSection from '@components/admin/CardSection';
import UserManagement from '@components/admin/UserManagement';
import BrandingMeta from '@components/admin/BrandingMeta';
import PunishmentSettings from '@components/admin/PunishmentSettings';
import DetailHistorySettings from '@components/admin/DetailHistorySettings';
import NewPunishmentModal from '@components/common/modals/NewPunishmentModal';
import Sidebar from '@components/common/sidebar/Sidebar';
import {useTranslation} from "react-i18next";
import Notification from "@components/common/Notification.tsx";

type PunishmentKey = keyof ServerConfig['punishments'];

const AdminPanelPage: React.FC = () => {
    const { t } = useTranslation();

    useTitle(t("admin.title"));

    const { serverConfig, isLoading: isConfigLoading } = useServerConfig();
    const { isAuthenticated, isLoading: isAuthLoading, user } = useAuth();
    const navigate = useNavigate();

    const { isLoading: isUsersLoading } = useUsers();

    const { punishmentsData } = usePunishmentsData();
    const { globalCounts } = punishmentsData;

    const [sidebarWidth, setSidebarWidth] = useState(280);
    const [showSuccessNotif, setShowSuccessNotif] = useState(false);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [isNewPunishmentModalOpen, setIsNewPunishmentModalOpen] = useState(false);
    const [isLogsModalOpen, setIsLogsModalOpen] = useState(false);
    const [minLoadingDone, setMinLoadingDone] = useState(false);

    const handleOpenNewPunishmentModal = useCallback(() => setIsNewPunishmentModalOpen(true), []);
    const handleCloseNewPunishmentModal = useCallback(() => setIsNewPunishmentModalOpen(false), []);

    const handleNewPunishmentSuccess = () => {
        setIsNewPunishmentModalOpen(false);
        setShowSuccessNotif(true);
    };

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
                serverName: serverConfig && serverConfig.serverName || '',
                primaryColor: serverConfig && serverConfig.serverColor || '#4f46e5',
                secondaryColor: serverConfig && serverConfig.serverColorHover || '#6366f1',
                logo: serverConfig && serverConfig.serverLogo || '',
                favicon: serverConfig && serverConfig.serverFavicon || '',
                description: serverConfig && serverConfig.serverDescription || '',
            },
            features: {
                allowExecution: (serverConfig && serverConfig.punishmentExecution) ?? true,
                allowRevocation: (serverConfig && serverConfig.punishmentRevocation) ?? true,
            },
            punishments: {
                bans: {
                    enabled: (serverConfig && serverConfig.punishments?.bans?.enabled) ?? true,
                    maxPerPage: (serverConfig && serverConfig.punishments?.bans?.maxPerPage) ?? 20,
                },
                mutes: {
                    enabled: (serverConfig && serverConfig.punishments?.mutes?.enabled) ?? true,
                    maxPerPage: (serverConfig && serverConfig.punishments?.mutes?.maxPerPage) ?? 20,
                },
                warnings: {
                    enabled: (serverConfig && serverConfig.punishments?.warnings?.enabled) ?? true,
                    maxPerPage: (serverConfig && serverConfig.punishments?.warnings?.maxPerPage) ?? 20,
                },
                kicks: {
                    enabled: (serverConfig && serverConfig.punishments?.kicks?.enabled) ?? true,
                    maxPerPage: (serverConfig && serverConfig.punishments?.kicks?.maxPerPage) ?? 20,
                },
            },
            details: {
                moderator: {
                    enabled: true,
                    maxPerPage: (serverConfig && serverConfig.histories?.moderatorMaxPerPage) ?? 20,
                },
                player: {
                    enabled: true,
                    maxPerPage: (serverConfig && serverConfig.histories?.playerMaxPerPage) ?? 20,
                },
                punishment: {
                    enabled: true,
                    revokeButton: (serverConfig && serverConfig.punishmentRevocation) ?? true,
                },
            },
        });
    }, [isConfigLoading, serverConfig]);

    const isAdmin = useMemo(() => !!user?.permissions?.includes('flexbans.web.admin'), [user]);

    useEffect(() => {
        if (!isAuthLoading && serverConfig && serverConfig.isSecured && !isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, isAuthLoading, serverConfig && serverConfig.isSecured, navigate]);

    useEffect(() => {
        const timer = setTimeout(() => {
            setMinLoadingDone(true);
        }, 200);

        return () => clearTimeout(timer);
    }, []);

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

    if (isAuthLoading || isConfigLoading || isUsersLoading || !form || !minLoadingDone) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-background text-text-primary">
                <div className="flex items-center space-x-3 p-8 rounded-2xl bg-surface shadow-xl">
                    <div
                        className="animate-spin rounded-full h-8 w-8 border-b-2 border-server-color"
                    />
                    <p className="text-xl font-semibold">{t("loading")}</p>
                </div>
            </div>
        );
    }

    if (serverConfig && serverConfig.isSecured && (!isAuthenticated || !isAdmin)) {
        return <Error403 />;
    }

    return (
        <div className={`flex min-h-screen bg-background text-text-primary`}>
            <Notification
                message={t("create-modal.success.message")}
                type={'success'}
                visible={showSuccessNotif}
                onClose={() => setShowSuccessNotif(false)}
            />

            <Sidebar
                counts={globalCounts}
                activeType={null}
                setActiveType={handleSidebarSetActiveType}
                isSidebarOpen={isSidebarOpen}
                setIsSidebarOpen={setIsSidebarOpen}
                onNewPunishmentClick={handleOpenNewPunishmentModal}
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
                <main className="p-4 sm:p-6 lg:p-8 space-y-8 flex-1 min-w-0">
                    <div className="flex items-start justify-between">
                        <div>
                            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">{t("admin.title")}</h1>
                            <p className="text-sm sm:text-base mt-2 text-text-secondary">
                                {t("admin.description")}
                            </p>
                        </div>
                        {serverConfig && serverConfig.isSecured && isAuthenticated && (
                            <div className="hidden lg:block mt-1">
                                <MyAccount onPreferencesApply={() => {}}/>
                            </div>
                        )}
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                        <MetricCard icon={<FaUsersCog />} label={t("admin.sections.metrics.total")} value="128" />
                        <MetricCard icon={<FaShieldAlt />} label={t("admin.sections.metrics.admins")} value="6" />
                        <MetricCard icon={<FaChartBar />} label={t("admin.sections.metrics.sessions")} value="42" />
                        <MetricCard icon={<FaCogs />} label={t("admin.sections.metrics.actions")} value="3" />
                    </div>

                    <CardSection title={t("admin.sections.user-management.title")} description={t("admin.sections.user-management.description")}>
                        <UserManagement searchTerm={searchTerm} onSearch={setSearchTerm} />
                    </CardSection>

                    <CardSection title={t("admin.sections.branding-meta.title")} description={t("admin.sections.branding-meta.description")}>
                        <BrandingMeta branding={form.branding} onChange={updateBranding} />
                    </CardSection>

                    <CardSection title={t("admin.sections.punishment-settings.title")} description={t("admin.sections.punishment-settings.description")}>
                        <PunishmentSettings
                            punishments={form.punishments}
                            features={form.features}
                            onUpdatePunishment={updatePunishment}
                            onUpdateFeature={updateFeature}
                        />
                    </CardSection>

                    <CardSection title={t("admin.sections.detail-history-settings.title")} description={t("admin.sections.detail-history-settings.description")}>
                        <DetailHistorySettings details={form.details} onUpdateDetails={updateDetails} />
                    </CardSection>

                    <div
                        className={`rounded-2xl border shadow-md px-5 py-4 flex items-center justify-between gap-4 
                                    bg-surface border-surface-border text-text-primary hover:border-border-active
                                    transition hover:-translate-y-0.5 active:translate-y-0`}
                        role="button"
                        tabIndex={0}
                        onClick={() => setIsLogsModalOpen(true)}
                        onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && setIsLogsModalOpen(true)}
                    >
                        <div className="flex items-center gap-3">
                            <div
                                className={`rounded-xl p-3 border bg-surface-elevated border-surface-border text-[#8ef0a3]`}
                            >
                                <FaTerminal className="text-lg" />
                            </div>
                            <div className="text-left">
                                <p className="text-sm font-semibold">{t("admin.logs.title")}</p>
                                <p className="text-xs text-text-secondary">{t("admin.logs.title")}</p>
                            </div>
                        </div>
                    </div>

                    <div className="flex flex-wrap justify-end gap-3">
                        <button className={`px-4 py-2 rounded-xl text-sm font-semibold bg-surface text-text-primary hover:bg-modal-surface-elevated transition duration-200 disabled:opacity-60`}>
                            {t("admin.buttons.cancel")}
                        </button>
                        <button
                            className="bg-server-color px-5 py-2 rounded-xl text-sm font-semibold text-text-primary shadow-md hover:scale-[1.01] active:scale-[0.99] transition"
                            onClick={() => {
                                alert('Settings saved (mock). Wire to your API/mutation here.');
                            }}
                        >
                            {t("admin.buttons.save")}
                        </button>
                    </div>
                </main>
            </div>

            {serverConfig && serverConfig.isSecured && user && isAuthenticated && (
                <NewPunishmentModal
                    isOpen={isNewPunishmentModalOpen}
                    onClose={handleCloseNewPunishmentModal}
                    onSuccess={handleNewPunishmentSuccess}
                    executorName={user.username}
                />
            )}

            <LogsModal
                isOpen={isLogsModalOpen}
                onClose={() => setIsLogsModalOpen(false)}
                logs={mockLogs}
            />
        </div>
    );
};

export default AdminPanelPage;