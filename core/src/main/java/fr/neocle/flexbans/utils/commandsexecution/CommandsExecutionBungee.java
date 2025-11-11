package fr.neocle.flexbans.utils.commandsexecution;

import net.md_5.bungee.api.ProxyServer;

public class CommandsExecutionBungee implements CommandsExecution {
    @Override
    public void executeCommand(String command) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), command);
    }
}
