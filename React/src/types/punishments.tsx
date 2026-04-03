export type PunishmentType = 'BAN' | 'MUTE' | 'WARNING' | 'KICK';
export type PunishmentStatus = 'Active' | 'Expired' | 'Removed';

export interface Punishment {
    database_id: number;
    punishment_id: string;
    type: PunishmentType;
    player: string;
    moderator: string;
    reason: string;
    status: PunishmentStatus | null;
    date: string;
    expiration_date: string;
    duration: string | null;
    server_scope: string;
}

export interface PunishmentDetailData {
    database_id: number;
    punishment_id: string;
    punishment_type: string;
    player: string;
    executor: string;
    reason: string;
    execution_date: string;
    expiration_date: string;
    duration: string | null;
    origin_server: string;
    scope_server: string;
    ip_scope: boolean;
    remover_name?: string | null;
    removal_reason?: string | null;
    status: 'Active' | 'Removed' | 'Expired' | null;
}

export interface PlayerStats {
    received: number;
    active?: number;
    removed?: number;
    expired?: number;
}

export interface StaffStats {
    sent: number;
    bansSent?: number;
    active?: number;
    removed?: number;
}

export type GlobalTypeCounts = Record<PunishmentType, number>;

export interface PunishmentsData {
    recentPunishments: Punishment[];
    userStats: Record<string, PlayerStats | StaffStats>;
    globalCounts: GlobalTypeCounts;
}