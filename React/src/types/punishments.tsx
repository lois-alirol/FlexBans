export type PunishmentType = 'BAN' | 'MUTE' | 'WARNING' | 'KICK';
export type PunishmentStatus = 'Active' | 'Expired' | 'Removed';

export interface Punishment {
  id: number;
  type: PunishmentType;
  player: string;
  moderator: string;
  reason: string;
  status: PunishmentStatus;
  date: string;
  duration: string;
}

export interface PlayerStats {
    received: number;
    active: number;
    removed: number;
    expired: number;
}

export interface StaffStats {
    sent: number;
    bansSent: number;
    active: number;
    removed: number;
}

export type GlobalTypeCounts = Record<PunishmentType, number>;

export interface PunishmentsData {
    recentPunishments: Punishment[];
    userStats: Record<string, PlayerStats | StaffStats>;
    globalCounts: GlobalTypeCounts;
}