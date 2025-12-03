package fr.neocle.flexbans.common.command.punishment.warning;

import java.util.List;

public interface IWarningCommandHelper {
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}
