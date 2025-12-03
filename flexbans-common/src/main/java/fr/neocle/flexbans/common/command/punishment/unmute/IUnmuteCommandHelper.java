package fr.neocle.flexbans.common.command.punishment.unmute;

import java.util.List;

public interface IUnmuteCommandHelper {
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}