package fr.neocle.flexbans.common.command.lookup.history;

import fr.neocle.flexbans.database.player.ProfilesManager;

import java.util.List;
import java.util.UUID;

public interface IHistoryCommandHelper {
    UUID getPlayerUuid(String playerName);
    String formatTime(long timestamp);
    List<String> getOnlinePlayerSuggestions(String partialName);
}