package fr.neocle.flexbans.common.command.server.unlock;

import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import java.util.List;

public interface IServerUnlockCommandHelper {
    String getDefaultServerName(ICommandSource source);
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}
