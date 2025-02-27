package fr.neocle.litebansweb.utils.CommandsExecution;

import com.velocitypowered.api.proxy.ProxyServer;

public class CommandsExecutionVelocity implements CommandsExecution {
    private ProxyServer proxyServer;

    public CommandsExecutionVelocity(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void executeCommand(String command) {
        if (proxyServer != null) {
            proxyServer.getCommandManager().executeAsync(proxyServer.getConsoleCommandSource(), command);
        } else {
            throw new IllegalStateException("ProxyServer is not initialized.");
        }
    }
}
