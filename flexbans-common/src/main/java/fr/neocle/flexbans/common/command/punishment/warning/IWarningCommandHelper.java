package fr.neocle.flexbans.common.command.punishment.warning;

import java.util.List;

public interface IWarningCommandHelper {
    void sendDialogMessage(String playerName, String dialogType);
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}
