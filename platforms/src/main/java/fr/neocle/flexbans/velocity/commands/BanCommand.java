package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class BanCommand implements SimpleCommand {
    private final BanExecutor banExecutor;
    private final ProxyServer proxyServer;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+(s|m|h|d|w|mo|y)$");

    public BanCommand(BanExecutor banExecutor, ProxyServer proxyServer) {
        this.banExecutor = banExecutor;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /ban <player/uuid> [duration] [reason] [-s] [-sender=<name>] [-scope=<server>]"));
            return;
        }

        String target = null;
        String duration = null;
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
            } else if (DURATION_PATTERN.matcher(arg).matches() && (duration == null || duration.isEmpty())) {
                duration = arg;
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

        if (!scope.equalsIgnoreCase("Global") && proxyServer.getServer(scope).isEmpty()) {
            source.sendMessage(Component.text("§cError: The specified server '" + scope + "' is not registered in Velocity."));
            return;
        }

        String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);

        banExecutor.executeBan(target, sender, duration, reason, scope, origin, silent, false, message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.ban");
    }
}
