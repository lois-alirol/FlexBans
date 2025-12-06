package fr.neocle.flexbans.common.command.lookup.history;

public record HistoryEntry(
        int id,
        String type,
        String issuerName,
        String reason,
        long time,
        String status,
        String removalReason,
        boolean ipScope
) {}