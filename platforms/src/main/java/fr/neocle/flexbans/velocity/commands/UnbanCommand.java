package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.punishments.unban.UnbanExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class UnbanCommand implements SimpleCommand {
    private final UnbanExecutor unbanExecutor;
    private final ProxyServer proxyServer;

    public UnbanCommand(UnbanExecutor unbanExecutor, ProxyServer proxyServer) {
        this.unbanExecutor = unbanExecutor;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /unban <player/uuid> [-s] [-sender=<name>]"));
            return;
        }

        String target = null;
        String sender = null;
        String scope = "Global";
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.startsWith("-sender=")) {
                sender = arg.substring(8);
            } else if (arg.startsWith("-scope=")) {
                scope = arg.substring(7);
            } else if (target == null) {
                target = arg;
            } else {
                reasonParts.add(arg);
            }
        }

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified."));
            return;
        }

        if (sender == null || sender.isEmpty()) {
            sender = source instanceof Player ? ((Player) source).getUsername() : "Console";
        }

        if (!scope.equalsIgnoreCase("Global") && proxyServer.getServer(scope).isEmpty()) {
            source.sendMessage(Component.text("§cError: The specified server '" + scope + "' is not registered in Velocity."));
            return;
        }

        String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);

        unbanExecutor.executeUnban(target, sender, reason, scope, silent, message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.unban");
    }
}
