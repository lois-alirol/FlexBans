package fr.neocle.flexbans.common.command.punishment.unban;

import java.util.List;

public interface IUnbanCommandHelper {
    boolean isServerRegistered(String serverName);
    List<String> getRegisteredServers();
    List<String> getOnlinePlayerNames();
}