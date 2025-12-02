package fr.neocle.flexbans.common.commands.punishments.ban;

import java.util.List;

public interface IBanCommandHelper {
    void sendDialogMessage(String playerName, String dialogType);
    boolean isServerRegistered(String serverName);
    List<String> getOnlinePlayerNames();
    List<String> getRegisteredServers();
}
