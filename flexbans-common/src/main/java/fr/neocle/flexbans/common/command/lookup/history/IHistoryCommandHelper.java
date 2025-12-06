package fr.neocle.flexbans.common.command.lookup.history;

import java.util.List;
import java.util.UUID;

public interface IHistoryCommandHelper {
    UUID getPlayerUuid(String playerName);
    List<HistoryEntry> getPlayerHistory(UUID playerUuid);
    String formatTime(long timestamp);
    List<String> getOnlinePlayerSuggestions(String partialName);
}