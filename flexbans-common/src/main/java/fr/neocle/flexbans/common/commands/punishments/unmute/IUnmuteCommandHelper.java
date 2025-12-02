package fr.neocle.flexbans.common.commands.punishments.unmute;

import java.util.List;

public interface IUnmuteCommandHelper {
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}