import React, { useState, useRef } from 'react';
import {
    FaPlus,
    FaSearch,
    FaSyncAlt,
    FaEdit,
    FaBan,
    FaKey,
    FaUsersCog,
    FaUserClock,
    FaCheck,
    FaExclamationTriangle,
    FaCheckCircle,
    FaTimesCircle,
    FaSpinner
} from 'react-icons/fa';

import { useAuth } from '@hooks/useAuth';
import { useUsers } from '@hooks/useUsers';
import {useTranslation} from "react-i18next";

import Notification from "@components/common/Notification.tsx";

interface Props {
    searchTerm: string;
    onSearch: (v: string) => void;
}

const VerificationBadge: React.FC<{ verified: boolean }> = ({ verified }) => {
    const { t } = useTranslation();

    return (
        <span
            className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-semibold ${
                verified
                    ? 'bg-blue-500/15 text-blue-500'
                    : 'bg-gray-500/15 text-gray-500'
            }`}
        >
      {verified ? (
          <>
              <FaCheckCircle className="text-[10px]" />
              {t("admin.sections.user-management.user.verified")}
          </>
      ) : (
          <>
              <FaTimesCircle className="text-[10px]" />
              {t("admin.sections.user-management.user.unverified")}
          </>
      )}
    </span>
    );
};

const humanReadableBlocked = (
    t: any,
    blockedAdd?: string[],
    blockedRemove?: string[]
) => {
    let msgs: string[] = [];

    if (blockedAdd && blockedAdd.length) {
        msgs.push(
            t("admin.sections.user-management.permissions.blocked-add", {
                permissions: blockedAdd.join(", "),
            })
        );
    }

    if (blockedRemove && blockedRemove.length) {
        msgs.push(
            t("admin.sections.user-management.permissions.blocked-remove", {
                permissions: blockedRemove.join(", "),
            })
        );
    }

    return t("admin.sections.user-management.permissions.update-error", {
        details: msgs.join(". "),
    });
};


const UserManagement: React.FC<Props> = ({ searchTerm, onSearch }) => {
    const { user } = useAuth();
    const { users, isLoading, error, updateUserPermissions } = useUsers();
    const { t } = useTranslation();

    const [editingIdx, setEditingIdx] = useState<number | null>(null);
    const [confirmRevokeIdx, setConfirmRevokeIdx] = useState<number | null>(null);
    const [permissionInputs, setPermissionInputs] = useState<Record<number, string>>({});
    const [stagedPermissionChanges, setStagedPermissionChanges] = useState<Record<number, { add: string[]; remove: string[] }>>({});
    const [notification, setNotification] = useState<{ message: React.ReactNode; type: 'error' | 'success' | 'info' } | null>(null);
    const [notificationVisible, setNotificationVisible] = useState<boolean>(false);
    const notificationTimeoutRef = useRef<number | null>(null);
    const revokeTimerRef = useRef<number | null>(null);

    const filteredUsers = users.filter(u =>
        u.username.toLowerCase().includes(searchTerm.toLowerCase())
    );

    const showNotification = (message: React.ReactNode, type: 'error' | 'success' | 'info' = 'error') => {
        setNotification({ message, type });
        setNotificationVisible(true);

        if (notificationTimeoutRef.current) clearTimeout(notificationTimeoutRef.current);
        notificationTimeoutRef.current = window.setTimeout(() => {
            setNotificationVisible(false);
            setTimeout(() => setNotification(null), 990);
        }, 5000);
    };

    const handleEdit = (idx: number) => {
        setEditingIdx(idx);
        setConfirmRevokeIdx(null);

        const user = filteredUsers[idx];
        if (user) {
            setStagedPermissionChanges(prev => ({
                ...prev,
                [user.id]: { add: [], remove: [] }
            }));
        }
    };

    const handleSave = async () => {
        if (editingIdx === null) return;
        const user = filteredUsers[editingIdx];
        if (!user) return;

        const staged = stagedPermissionChanges[user.id];
        if (staged && (staged.add.length || staged.remove.length)) {
            try {
                const res = await updateUserPermissions(user.username, staged.add, staged.remove);
                if (res && typeof res === 'object' && ('blockedAdd' in res || 'blockedRemove' in res)) {
                    const blockedAdd = (res as any).blockedAdd ?? [];
                    const blockedRemove = (res as any).blockedRemove ?? [];
                    if (blockedAdd.length || blockedRemove.length) {
                        showNotification(humanReadableBlocked(t, blockedAdd, blockedRemove), 'error');
                    }
                }
            } catch (e: any) {
                const serverError = e?.serverError ?? {};
                const blockedAdd = serverError?.data?.blockedAdd ?? [];
                const blockedRemove = serverError?.data?.blockedRemove ?? [];
                if ((blockedAdd && blockedAdd.length) || (blockedRemove && blockedRemove.length)) {
                    showNotification(humanReadableBlocked(t, blockedAdd, blockedRemove), 'error');
                } else if (serverError?.message) {
                    showNotification(serverError.message, 'error');
                } else {
                    showNotification(e.message || t("admin.sections.user-management.permissions.fallback-error"), 'error');
                }
            }
        }

        setEditingIdx(null);
        setStagedPermissionChanges(prev => {
            const clone = { ...prev };
            delete clone[user.id];
            return clone;
        });
        setPermissionInputs(prev => {
            const clone = { ...prev };
            delete clone[user.id];
            return clone;
        });
    };

    const handleRevokeClick = (idx: number) => {
        if (confirmRevokeIdx === idx) {
            setConfirmRevokeIdx(null);
            if (revokeTimerRef.current) clearTimeout(revokeTimerRef.current);
        } else {
            setConfirmRevokeIdx(idx);
            setEditingIdx(null);

            if (revokeTimerRef.current) clearTimeout(revokeTimerRef.current);
            revokeTimerRef.current = window.setTimeout(() => {
                setConfirmRevokeIdx(null);
            }, 5000);
        }
    };

    const handleStageAddPermission = (userId: number, permission: string) => {
        const trimmed = permission.trim();
        if (!trimmed) return;
        setStagedPermissionChanges(prev => {
            const curr = prev[userId] || { add: [], remove: [] };
            return {
                ...prev,
                [userId]: {
                    add: curr.add.includes(trimmed) ? curr.add : [...curr.add, trimmed],
                    remove: curr.remove.filter(r => r !== trimmed)
                }
            };
        });
        setPermissionInputs(prev => ({ ...prev, [userId]: '' }));
    };

    const handleStageRemovePermission = (userId: number, permission: string) => {
        setStagedPermissionChanges(prev => {
            const curr = prev[userId] || { add: [], remove: [] };
            return {
                ...prev,
                [userId]: {
                    add: curr.add.filter(a => a !== permission),
                    remove: curr.remove.includes(permission) ? curr.remove : [...curr.remove, permission]
                }
            };
        });
    };

    const activeCount = filteredUsers.filter((u) => u.verified).length;
    const suspendedCount = filteredUsers.filter((u) => !u.verified).length;

    const statShell = 'bg-surface-elevated border-surface-border text-text-primary';
    const mutedText = 'text-text-secondary';

    if (error) {
        return (
            <div className={`text-center py-12 rounded-xl border border-error-border bg-error/20 `}>
                <FaExclamationTriangle className="mx-auto text-4xl text-error mb-3" />
                <p className={`text-lg font-semibold text-text-error`}>
                    {t("admin.sections.user-management.error-loading")}
                </p>
                <p className={`text-sm text-text-error mt-1`}>
                    {error}
                </p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            {notification && (
                <Notification
                    message={notification.message}
                    type={notification.type}
                    visible={notificationVisible}
                    onClose={() => { setNotificationVisible(false); setNotification(null); }}
                />
            )}

            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div className="relative grow">
                    <input
                        type="text"
                        placeholder={t("admin.sections.user-management.search.placeholder")}
                        value={searchTerm}
                        onChange={(e) => onSearch(e.target.value)}
                        className={`w-full p-3 pl-11 rounded-xl shadow-md focus:outline-none focus:ring-2 transition-all duration-200
                                    bg-surface-elevated border border-surface-border text-text-primary focus:ring-server-color/30`}
                    />
                    <FaSearch className="absolute left-3.5 top-1/2 transform -translate-y-1/2 text-text-secondary" />
                </div>

                <div className="flex flex-wrap gap-3">
                    <button
                        className="bg-server-color flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold text-text-primary shadow-md hover:scale-[1.02] active:scale-[0.98] transition"
                    >
                        <FaPlus /> {t("admin.sections.user-management.buttons.invite")}
                    </button>

                    <button
                        className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold 
                                    bg-surface-elevated text-text-primary hover:bg-surface-elevated/40 transition`}
                        onClick={() => window.location.reload()}
                    >
                        <FaSyncAlt className={isLoading ? 'animate-spin' : ''} /> {t("admin.sections.user-management.buttons.refresh")}
                    </button>
                </div>
            </div>

            <div className="flex flex-wrap gap-3">
                <div className={`flex items-center gap-3 px-4 py-3 rounded-2xl shadow-sm border ${statShell}`}>
                    <div className="bg-server-color/30 text-server-color h-11 w-11 flex items-center justify-center rounded-xl">
                        <FaUsersCog />
                    </div>
                    <div>
                        <p className={`text-[11px] uppercase tracking-wide font-semibold ${mutedText}`}>{t("admin.sections.user-management.user.verified")}</p>
                        <p className="text-2xl font-black leading-tight">{activeCount}</p>
                    </div>
                </div>

                <div className={`flex items-center gap-3 px-4 py-3 rounded-2xl shadow-sm border ${statShell}`}>
                    <div className="h-11 w-11 flex items-center justify-center rounded-xl text-amber-600" style={{ backgroundColor: '#f59e0b1a' }}>
                        <FaUserClock />
                    </div>
                    <div>
                        <p className={`text-[11px] uppercase tracking-wide font-semibold ${mutedText}`}>{t("admin.sections.user-management.user.unverified")}</p>
                        <p className="text-2xl font-black leading-tight">{suspendedCount}</p>
                    </div>
                </div>
            </div>

            {isLoading ? (
                <div className={`text-center py-12 rounded-xl border
                                border-surface-border bg-surface-elevated`}>
                    <FaSpinner className="mx-auto text-4xl opacity-30 mb-3 animate-spin" />
                    <p className={`text-lg font-semibold text-text-primary`}>
                        {t("loading")}
                    </p>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                    {filteredUsers.map((u, idx) => {
                        const isEditing = editingIdx === idx;
                        const isConfirmingRevoke = confirmRevokeIdx === idx;
                        const isCurrentUser = user?.username === u.username;
                        const newPermission = permissionInputs[u.id] || '';

                        const staged = stagedPermissionChanges[u.id] || { add: [], remove: [] };

                        let renderedPermissions = u.permissions.filter(
                            perm => !staged.remove.includes(perm)
                        );
                        for (const permToAdd of staged.add) {
                            if (!renderedPermissions.includes(permToAdd)) {
                                renderedPermissions = [...renderedPermissions, permToAdd];
                            }
                        }

                        return (
                            <div
                                key={u.id || idx}
                                className={`flex flex-col gap-3 p-4 rounded-xl border transition-all duration-200
                                            border-surface-border bg-surface-elevated 
                                            ${isEditing ? 'animate-wiggle-soft border-2 ring-2 ring-server-color/75 border-server-color' : ''} 
                                            ${isConfirmingRevoke ? 'ring-2 ring-rose-500/50 border-rose-500' : ''}`}
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div className="flex-1">
                                        <p className="text-lg font-semibold flex items-center">
                                            {u.username}
                                            {isCurrentUser && (
                                                <span className="ml-2 text-sm font-normal opacity-70">
                                                    ({t("admin.sections.user-management.user.you")})
                                                </span>
                                            )}
                                        </p>
                                        <div className="flex flex-wrap items-center gap-2 mt-1">
                                            <VerificationBadge verified={u.verified} />
                                        </div>
                                    </div>

                                    <div className="flex gap-2">
                                        <button
                                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 border ${
                                                !isEditing
                                                    ? 'border-blue-500/30 bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 hover:border-blue-500/50'
                                                    : 'border-green-500/30 bg-green-500/10 text-green-400 hover:bg-green-500/20 hover:border-green-500/50'
                                            }`}
                                            onClick={() => (isEditing ? handleSave() : handleEdit(idx))}
                                        >
                                            {isEditing ? <><FaCheck className="text-xs" /> {t("admin.sections.user-management.user.buttons.confirm-edit")}</> : <><FaEdit className="text-xs" /> {t("admin.sections.user-management.user.buttons.edit")}</>}
                                        </button>

                                        <button
                                            onClick={() => handleRevokeClick(idx)}
                                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-300 border ${
                                                isConfirmingRevoke
                                                    ? 'bg-rose-600 text-white border-rose-600 animate-pulse'
                                                    : 'border-rose-500/30 bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 hover:border-rose-500/50'
                                            }`}
                                        >
                                            {isConfirmingRevoke ? (
                                                <><FaExclamationTriangle className="text-xs" /> {t("admin.sections.user-management.user.buttons.confirm-revoke")}</>
                                            ) : (
                                                <><FaBan className="text-xs" /> {t("admin.sections.user-management.user.buttons.revoke")}</>
                                            )}
                                        </button>
                                    </div>
                                </div>

                                <div className="space-y-3">
                                    <div className="flex items-center gap-2 text-sm font-semibold opacity-70">
                                        <FaKey className="text-xs" /> {t("admin.sections.user-management.user.permissions-count", {count: renderedPermissions.length})}
                                    </div>

                                    <div className="flex flex-wrap gap-y-3 gap-x-2">
                                        {renderedPermissions.length > 0 ? (
                                            renderedPermissions.map((perm, i) => (
                                                <span
                                                    key={`${perm}-${i}`}
                                                    className={`relative inline-flex items-center px-3 py-1 rounded-lg text-xs font-bold border 
                                                                transition-all bg-surface-elevated/40 border-surface-border text-text-primary 
                                                                ${isEditing ? 'pr-5' : ''}
                                                              `}
                                                >
                                                    {perm}
                                                    {isEditing && (
                                                        <button
                                                            type="button"
                                                            className="absolute -top-1.5 -right-1.5 h-4 w-4 flex items-center justify-center rounded-full bg-red-500 text-text-primary shadow-lg hover:bg-red-600 transition-colors border border-surface-border z-10"
                                                            onClick={() => handleStageRemovePermission(u.id, perm)}
                                                        >
                                                            <span className="text-[10px] leading-none font-medium">x</span>
                                                        </button>
                                                    )}
                                                </span>
                                            ))
                                        ) : (
                                            <span className={`text-xs italic text-text-secondary`}>
                                                {t("admin.sections.user-management.user.no-permissions")}
                                            </span>
                                        )}
                                    </div>

                                    {isEditing && (
                                        <div className="flex gap-2 pt-2">
                                            <input
                                                type="text"
                                                placeholder={t("admin.sections.user-management.user.add-permission")}
                                                value={newPermission}
                                                onChange={(e) => setPermissionInputs(prev => ({ ...prev, [u.id]: e.target.value }))}
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') {
                                                        handleStageAddPermission(u.id, newPermission);
                                                    }
                                                }}
                                                className={`w-full px-3 py-2 rounded-lg border text-sm focus:outline-none focus:ring-2 transition-all duration-200
                                                            bg-surface-elevated border-surface-border text-text-primary focus:ring-server-color`}
                                            />
                                            <button
                                                className="bg-server-color px-4 py-2 text-sm font-bold rounded-lg text-text-primary shadow-md active:scale-95 transition"
                                                onClick={() => handleStageAddPermission(u.id, newPermission)}
                                            >
                                                {t("admin.sections.user-management.buttons.add")}
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {!isLoading && filteredUsers.length === 0 && (
                <div className={`text-center py-12 rounded-xl border
                                 border-surface-border bg-surface-elevated`}>
                    <FaUsersCog className="mx-auto text-4xl opacity-30 mb-3" />
                    <p className={`text-lg font-semibold text-text-primary`}>
                        {t("admin.sections.user-management.search.no-results-title")}
                    </p>
                    <p className={`text-sm text-text-secondary mt-1`}>
                        {t("admin.sections.user-management.search.no-results-description")}
                    </p>
                </div>
            )}
        </div>
    );
};

export default UserManagement;