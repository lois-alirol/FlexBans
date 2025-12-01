package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.punishments.mute.MuteExecutor;
import fr.neocle.flexbans.velocity.commands.utils.PluginMessageUtil;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MuteCommand implements SimpleCommand {
    private final MuteExecutor muteExecutor;
    private final ProxyServer proxyServer;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+(s|m|h|d|w|mo|y)$");

    public MuteCommand(MuteExecutor muteExecutor, ProxyServer proxyServer) {
        this.muteExecutor = muteExecutor;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            if (source instanceof Player player) {
                if (player.getCurrentServer().isEmpty()) {
                    source.sendMessage(Component.text("§cYou must be connected to a server to use this command without arguments."));
                    return;
                }

                PluginMessageUtil.sendDialogMessage(player, "mute");
                return;
            }

            source.sendMessage(Component.text("Usage: /mute <player/uuid> [duration] [reason] [-s] [-sender=<name>] [-scope=<server>]"));
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

        muteExecutor.executeMute(target, sender, duration, reason, scope, origin, silent, false, message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.mute");
    }
}