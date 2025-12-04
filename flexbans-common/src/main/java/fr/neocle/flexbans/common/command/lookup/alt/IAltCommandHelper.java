package fr.neocle.flexbans.common.command.lookup.alt;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public interface IAltCommandHelper {
    InetAddress getPlayerInetAddress(String playerName);
    boolean isPlayerOnline(UUID playerUuid);
    List<String> getOnlinePlayerSuggestions(String partialName);
}