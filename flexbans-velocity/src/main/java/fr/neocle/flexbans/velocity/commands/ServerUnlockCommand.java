package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.server.unlock.ServerUnlockExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ServerUnlockCommand implements SimpleCommand {
    private final ServerUnlockExecutor serverUnlockExecutor;
    private final ProxyServer proxyServer;

    public ServerUnlockCommand(ServerUnlockExecutor serverUnlockExecutor, ProxyServer proxyServer) {
        this.serverUnlockExecutor = serverUnlockExecutor;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        String serverName = null;
        String sender = null;
        boolean silent = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.startsWith("-sender=")) {
                sender = arg.substring(8);
            } else if (serverName == null) {
                serverName = arg;
            }
        }

        if (serverName == null || serverName.isEmpty()) {
            if (source instanceof Player) {
                serverName = ((Player) source).getCurrentServer()
                        .map(server -> server.getServerInfo().getName())
                        .orElse("Global");
            } else {
                serverName = "Global";
            }
        }

        if (!serverName.equalsIgnoreCase("Global") && proxyServer.getServer(serverName).isEmpty()) {
            source.sendMessage(Component.text("§cError: The specified server '" + serverName + "' is not registered in Velocity."));
            return;
        }

        if (sender == null || sender.isEmpty()) {
            sender = source instanceof Player ? ((Player) source).getUsername() : "Console";
        }

        serverUnlockExecutor.unlockServer(serverName, sender, silent, message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            proxyServer.getAllServers().forEach(server ->
                    suggestions.add(server.getServerInfo().getName()));
        } else if (args.length > 1) {
            suggestions.add("-sender=");
        }

        return suggestions;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.serverlock.unlock");
    }
}