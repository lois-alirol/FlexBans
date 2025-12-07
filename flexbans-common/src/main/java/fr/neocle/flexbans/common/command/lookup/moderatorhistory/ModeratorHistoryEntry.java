package fr.neocle.flexbans.common.command.lookup.moderatorhistory;

public record ModeratorHistoryEntry(
        int id,
        String action,
        String type,
        String targetName,
        String reason,
        long timestamp,
        long duration,
        String status,
        String removedBy,
        boolean ipScope
) {}