package fr.neocle.flexbans.common.commands.server.lock;

import fr.neocle.flexbans.common.commands.ICommandSource;

import java.util.List;

public interface IServerLockCommandHelper {
    String getDefaultServerName(ICommandSource source);
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}