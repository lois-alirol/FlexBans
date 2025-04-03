package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.punishments.kick.KickExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class KickCommand implements SimpleCommand {
    private final KickExecutor kickExecutor;
    private final ProxyServer proxyServer;

    public KickCommand(KickExecutor kickExecutor, ProxyServer proxyServer) {
        this.kickExecutor = kickExecutor;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /kick <player/uuid> [reason] [-s] [-sender=<name>]"));
            return;
        }

        String target = null;
        String sender = null;
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.startsWith("-sender=")) {
                sender = arg.substring(8);
            } else if (target == null) {
                target = arg;
            } else {
                reasonParts.add(arg);
            }
        }

        String origin = (source instanceof Player)
                ? ((Player) source).getCurrentServer()
                .map(server -> server.getServer().getServerInfo().getName())
                .orElse("Proxy")
                : "Proxy";

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified."));
            return;
        }

        if (sender == null || sender.isEmpty()) {
            sender = source instanceof Player ? ((Player) source).getUsername() : "Console";
        }

        String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);

        kickExecutor.executeKick(target, sender, reason, origin, silent, false, message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("kick.use");
    }
}