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
    FaSpinner,
    FaTimes
} from 'react-icons/fa';
import type { Theme } from "../../types/admin.tsx";
import { useAuth } from "../../hooks/useAuth.ts";
import { useUsers } from "../../hooks/useUsers.ts";

// Notification with fade animation
const Notification: React.FC<{
    message: React.ReactNode,
    type?: 'error' | 'success' | 'info',
    onClose: () => void,
    visible: boolean,
    theme: Theme
}> = ({ message, type = 'error', onClose, visible, theme }) => (
    <div
        className={`fixed z-50 top-5 left-1/2 transform -translate-x-1/2 min-w-[300px] max-w-[96vw] px-4 py-3 rounded-xl shadow-md flex items-center gap-3 transition-opacity duration-1000
            ${
            type === 'error'
                ? theme === 'dark'
                    ? 'bg-rose-700 text-white border border-rose-900'
                    : 'bg-rose-100 text-rose-900 border border-rose-200'
                : 'bg-blue-100 text-blue-900 border border-blue-200'
        }
            ${visible ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}
        `}
    >
        <FaExclamationTriangle className="text-xl mr-2" />
        <span className="flex-1">{message}</span>
        <button className="ml-2 text-lg p-1 rounded hover:bg-black/10" onClick={onClose} aria-label="Dismiss">
            <FaTimes />
        </button>
    </div>
);

interface Props {
    searchTerm: string;
    onSearch: (v: string) => void;
    accent: string;
    theme: Theme;
}

const statusChip = (status: string) => {
    const isActive = status === 'Active';

    return (
        <span
            className={`px-2 py-1 rounded-full text-xs font-semibold ${
                isActive ? 'bg-green-500/15 text-green-500' : 'bg-red-500/15 text-red-500'
            }`}
        >
      {status}
    </span>
    );
};

const verificationBadge = (verified: boolean) => {
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
              Verified
          </>
      ) : (
          <>
              <FaTimesCircle className="text-[10px]" />
              Unverified
          </>
      )}
    </span>
    );
};

const UserManagement: React.FC<Props> = ({ searchTerm, onSearch, accent, theme }) => {
    const { user } = useAuth();
    const { users, isLoading, error, updateUserPermissions } = useUsers();

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

    // Fade-out notification UX
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

    const humanReadableBlocked = (blockedAdd?: string[], blockedRemove?: string[]) => {
        let msgs: string[] = [];
        if (blockedAdd && blockedAdd.length)
            msgs.push(`Could not add:   ${blockedAdd.join(', ')}`);
        if (blockedRemove && blockedRemove.length)
            msgs.push(`Could not remove: ${blockedRemove.join(', ')}`);
        return `Unable to update user's permissions! ${msgs.join('. ')}`;
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
                        showNotification(humanReadableBlocked(blockedAdd, blockedRemove), 'error');
                    }
                }
            } catch (e: any) {
                const serverError = e?.serverError ?? {};
                const blockedAdd = serverError?.data?.blockedAdd ?? [];
                const blockedRemove = serverError?.data?.blockedRemove ?? [];
                if ((blockedAdd && blockedAdd.length) || (blockedRemove && blockedRemove.length)) {
                    showNotification(humanReadableBlocked(blockedAdd, blockedRemove), 'error');
                } else if (serverError?.message) {
                    showNotification(serverError.message, 'error');
                } else {
                    showNotification(e.message || "Failed to update permissions", 'error');
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

    const statShell = theme === 'dark' ? 'bg-[#1f1f1f] ring-white/10 text-gray-100' : 'bg-white ring-black/5 text-gray-900';
    const mutedText = theme === 'dark' ? 'text-gray-400' : 'text-gray-500';

    if (error) {
        return (
            <div className={`text-center py-12 rounded-xl border ${
                theme === 'dark' ? 'border-red-500/30 bg-red-500/10' : 'border-red-200 bg-red-50'
            }`}>
                <FaExclamationTriangle className="mx-auto text-4xl text-red-500 mb-3" />
                <p className={`text-lg font-semibold ${theme === 'dark' ? 'text-red-400' : 'text-red-700'}`}>
                    Error loading users
                </p>
                <p className={`text-sm ${theme === 'dark' ? 'text-red-300' : 'text-red-600'} mt-1`}>
                    {error}
                </p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-4">
            <style>{`
        @keyframes wiggle-soft {
          0% { transform: rotate(-0.5deg); }
          50% { transform: rotate(0.5deg); }
          100% { transform: rotate(-0.5deg); }
        }
        .animate-wiggle-soft {
          animation: wiggle-soft 0.8s ease-in-out infinite;
        }
      `}</style>

            {notification && (
                <Notification
                    message={notification.message}
                    type={notification.type}
                    visible={notificationVisible}
                    theme={theme}
                    onClose={() => { setNotificationVisible(false); setNotification(null); }}
                />
            )}

            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div className="relative grow">
                    <input
                        type="text"
                        placeholder="Search users..."
                        value={searchTerm}
                        onChange={(e) => onSearch(e.target.value)}
                        className={`w-full p-3 pl-11 rounded-xl shadow-md focus:outline-none focus:ring-2 transition-all duration-200 ${
                            theme === 'dark' ? 'bg-[#2c2c2c] border border-gray-700 text-white' : 'bg-white border border-gray-200 text-gray-800'
                        }`}
                        style={({ '--tw-ring-color': `${accent}66` } as React.CSSProperties)}
                    />
                    <FaSearch className="absolute left-3.5 top-1/2 transform -translate-y-1/2 text-gray-400" />
                </div>

                <div className="flex flex-wrap gap-3">
                    <button
                        className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold text-white shadow-md hover:scale-[1.02] active:scale-[0.98] transition"
                        style={{ backgroundColor: accent }}
                    >
                        <FaPlus /> Invite User
                    </button>

                    <button
                        className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold ${
                            theme === 'dark' ? 'bg-[#2c2c2c] text-white hover:bg-[#363636]' : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                        } shadow-sm hover:scale-[1.02] transition`}
                        onClick={() => window.location.reload()}
                    >
                        <FaSyncAlt className={isLoading ? 'animate-spin' : ''} /> Refresh
                    </button>
                </div>
            </div>

            <div className="flex flex-wrap gap-3">
                <div className={`flex items-center gap-3 px-4 py-3 rounded-2xl shadow-sm ring-1 ${statShell}`}>
                    <div className="h-11 w-11 flex items-center justify-center rounded-xl" style={{ backgroundColor: `${accent}1a`, color: accent }}>
                        <FaUsersCog />
                    </div>
                    <div>
                        <p className={`text-[11px] uppercase tracking-wide font-semibold ${mutedText}`}>Verified</p>
                        <p className="text-2xl font-black leading-tight">{activeCount}</p>
                    </div>
                </div>

                <div className={`flex items-center gap-3 px-4 py-3 rounded-2xl shadow-sm ring-1 ${statShell}`}>
                    <div className="h-11 w-11 flex items-center justify-center rounded-xl text-amber-600" style={{ backgroundColor: '#f59e0b1a' }}>
                        <FaUserClock />
                    </div>
                    <div>
                        <p className={`text-[11px] uppercase tracking-wide font-semibold ${mutedText}`}>Unverified</p>
                        <p className="text-2xl font-black leading-tight">{suspendedCount}</p>
                    </div>
                </div>
            </div>

            {isLoading ? (
                <div className={`text-center py-12 rounded-xl border ${
                    theme === 'dark' ? 'border-gray-700 bg-[#1f1f1f]' : 'border-gray-200 bg-white'
                }`}>
                    <FaSpinner className="mx-auto text-4xl opacity-30 mb-3 animate-spin" />
                    <p className={`text-lg font-semibold ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
                        Loading users...
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
                                className={`flex flex-col gap-3 p-4 rounded-xl border shadow-sm transition-all duration-200 ${
                                    theme === 'dark' ? 'border-gray-700 bg-[#1f1f1f]' : 'border-gray-200 bg-white'
                                } ${isEditing ? 'animate-wiggle-soft border-2 ring-2' : ''} ${isConfirmingRevoke ? 'ring-2 ring-rose-500/50 border-rose-500' : ''}`}
                                style={isEditing ? {
                                    borderColor: accent,
                                    '--tw-ring-color': `${accent}4d`
                                } as React.CSSProperties : {}}
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div className="flex-1">
                                        <p className="text-lg font-semibold flex items-center">
                                            {u.username}
                                            {isCurrentUser && (
                                                <span className="ml-2 text-sm font-normal opacity-70">
                            (You)
                          </span>
                                            )}
                                        </p>
                                        <div className="flex flex-wrap items-center gap-2 mt-1">
                                            {statusChip("Active")}
                                            {verificationBadge(u.verified)}
                                        </div>
                                    </div>

                                    <div className="flex gap-2">
                                        <button
                                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-200 border ${
                                                !isEditing
                                                    ? theme === 'dark'
                                                        ? 'border-blue-500/30 bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 hover:border-blue-500/50'
                                                        : 'border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-600 hover:text-white'
                                                    : theme === 'dark'
                                                        ? 'border-green-500/30 bg-green-500/10 text-green-400 hover:bg-green-500/20 hover:border-green-500/50'
                                                        : 'border-green-200 bg-green-50 text-green-700 hover:bg-green-600 hover:text-white'
                                            }`}
                                            onClick={() => (isEditing ? handleSave() : handleEdit(idx))}
                                        >
                                            {isEditing ? <><FaCheck className="text-xs" /> Save</> : <><FaEdit className="text-xs" /> Edit</>}
                                        </button>

                                        <button
                                            onClick={() => handleRevokeClick(idx)}
                                            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-300 border ${
                                                isConfirmingRevoke
                                                    ? 'bg-rose-600 text-white border-rose-600 animate-pulse'
                                                    : theme === 'dark'
                                                        ? 'border-rose-500/30 bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 hover:border-rose-500/50'
                                                        : 'border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-600 hover:text-white'
                                            }`}
                                        >
                                            {isConfirmingRevoke ? (
                                                <><FaExclamationTriangle className="text-xs" /> Confirm?</>
                                            ) : (
                                                <><FaBan className="text-xs" /> Revoke Access</>
                                            )}
                                        </button>
                                    </div>
                                </div>

                                <div className="space-y-3">
                                    <div className="flex items-center gap-2 text-sm font-semibold opacity-70">
                                        <FaKey className="text-xs" /> Permissions {renderedPermissions.length > 0 && `(${renderedPermissions.length})`}
                                    </div>

                                    <div className="flex flex-wrap gap-y-3 gap-x-2">
                                        {renderedPermissions.length > 0 ? (
                                            renderedPermissions.map((perm, i) => (
                                                <span
                                                    key={`${perm}-${i}`}
                                                    className={`relative inline-flex items-center px-3 py-1 rounded-lg text-xs font-bold border transition-all ${
                                                        theme === 'dark'
                                                            ? 'bg-[#2a2a2a] border-gray-700 text-gray-300'
                                                            : 'bg-gray-100 border-gray-200 text-gray-700'
                                                    } ${isEditing ? 'pr-5' : ''}`}
                                                >
                                                    {perm}
                                                    {isEditing && (
                                                        <button
                                                            type="button"
                                                            className="absolute -top-1.5 -right-1.5 h-4 w-4 flex items-center justify-center rounded-full bg-red-500 text-white shadow-lg hover:bg-red-600 transition-colors border border-white dark:border-[#1f1f1f] z-10"
                                                            onClick={() => handleStageRemovePermission(u.id, perm)}
                                                        >
                                                            <span className="text-[10px] leading-none font-black">✕</span>
                                                        </button>
                                                    )}
                                                </span>
                                            ))
                                        ) : (
                                            <span className={`text-xs italic ${theme === 'dark' ? 'text-gray-500' : 'text-gray-400'}`}>
                                                No permissions assigned
                                            </span>
                                        )}
                                    </div>

                                    {isEditing && (
                                        <div className="flex gap-2 pt-2">
                                            <input
                                                type="text"
                                                placeholder="Add permission..."
                                                value={newPermission}
                                                onChange={(e) => setPermissionInputs(prev => ({ ...prev, [u.id]: e.target.value }))}
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') {
                                                        handleStageAddPermission(u.id, newPermission);
                                                    }
                                                }}
                                                className={`w-full px-3 py-2 rounded-lg border text-sm focus:outline-none focus:ring-2 transition-all duration-200 ${
                                                    theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'
                                                }`}
                                                style={({ '--tw-ring-color': `${accent}66`, borderColor: accent } as React.CSSProperties)}
                                            />
                                            <button
                                                className="px-4 py-2 text-sm font-bold rounded-lg text-white shadow-md active:scale-95 transition"
                                                style={{ backgroundColor: accent }}
                                                onClick={() => handleStageAddPermission(u.id, newPermission)}
                                            >
                                                Add
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
                <div className={`text-center py-12 rounded-xl border ${
                    theme === 'dark' ? 'border-gray-700 bg-[#1f1f1f]' : 'border-gray-200 bg-white'
                }`}>
                    <FaUsersCog className="mx-auto text-4xl opacity-30 mb-3" />
                    <p className={`text-lg font-semibold ${theme === 'dark' ? 'text-gray-300' : 'text-gray-700'}`}>
                        No users found
                    </p>
                    <p className={`text-sm ${theme === 'dark' ? 'text-gray-500' : 'text-gray-400'} mt-1`}>
                        {searchTerm ? 'Try adjusting your search terms' : 'Start by inviting a user'}
                    </p>
                </div>
            )}
        </div>
    );
};

export default UserManagement;