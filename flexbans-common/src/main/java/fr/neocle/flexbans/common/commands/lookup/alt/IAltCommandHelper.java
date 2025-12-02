package fr.neocle.flexbans.common.commands.lookup.alt;

import java.util.List;
import java.util.UUID;

public interface IAltCommandHelper {
    String getPlayerIP(String playerName);
    boolean isPlayerOnline(UUID playerUuid);
    List<String> getOnlinePlayerSuggestions(String partialName);
}