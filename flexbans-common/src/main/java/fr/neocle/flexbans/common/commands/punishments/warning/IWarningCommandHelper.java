package fr.neocle.flexbans.common.commands.punishments.warning;

import java.util.List;

public interface IWarningCommandHelper {
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}
