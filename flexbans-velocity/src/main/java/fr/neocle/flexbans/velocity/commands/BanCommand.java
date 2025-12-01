package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import fr.neocle.flexbans.velocity.commands.utils.PluginMessageUtil;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BanCommand implements SimpleCommand {
    private final BanExecutor banExecutor;
    private final ProxyServer proxyServer;
    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+(s|m|h|d|w|mo|y)$");

    private static final List<String> DURATION_SUGGESTIONS = List.of(
            "1h", "2h", "6h", "12h",
            "1d", "2d", "3d", "7d", "14d", "30d",
            "1w", "2w", "4w",
            "1mo", "3mo", "6mo",
            "1y"
    );

    private static final List<String> REASON_SUGGESTIONS = List.of(
            "Cheating", "Hacking", "Griefing", "Toxicity",
            "Spam", "Advertising", "Exploiting", "Inappropriate behavior",
            "Harassment", "Racism", "Threats"
    );

    public BanCommand(BanExecutor banExecutor, ProxyServer proxyServer) {
        this.banExecutor = banExecutor;
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

                PluginMessageUtil.sendDialogMessage(player, "ban");
                return;
            }

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
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return getOnlinePlayerNames();
        }

        String currentArg = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return filterSuggestions(getOnlinePlayerNames(), currentArg);
        }

        if (currentArg.startsWith("-sender=")) {
            String prefix = "-sender=";
            String partial = currentArg.substring(prefix.length());
            return getOnlinePlayerNames().stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        if (currentArg.startsWith("-scope=")) {
            String prefix = "-scope=";
            String partial = currentArg.substring(prefix.length());
            List<String> suggestions = new ArrayList<>();
            suggestions.add(prefix + "Global");
            proxyServer.getAllServers().stream()
                    .map(server -> prefix + server.getServerInfo().getName())
                    .forEach(suggestions::add);
            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        if (currentArg.startsWith("-")) {
            List<String> flags = new ArrayList<>();
            flags.add("-s");
            flags.add("-sender=");
            flags.add("-scope=");
            return filterSuggestions(flags, currentArg);
        }

        boolean hasDuration = false;
        boolean hasFlag = false;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                hasFlag = true;
            }
            if (DURATION_PATTERN.matcher(arg).matches()) {
                hasDuration = true;
            }
        }

        if (args.length == 2 && !hasDuration && !hasFlag) {
            List<String> suggestions = new ArrayList<>(DURATION_SUGGESTIONS);
            suggestions.addAll(REASON_SUGGESTIONS);
            suggestions.add("-s");
            suggestions.add("-sender=");
            suggestions.add("-scope=");
            return filterSuggestions(suggestions, currentArg);
        }

        if (args.length >= 2 && !hasDuration && !hasFlag) {
            List<String> suggestions = new ArrayList<>(DURATION_SUGGESTIONS);
            suggestions.addAll(REASON_SUGGESTIONS);
            suggestions.add("-s");
            suggestions.add("-sender=");
            suggestions.add("-scope=");
            return filterSuggestions(suggestions, currentArg);
        }

        List<String> suggestions = new ArrayList<>(REASON_SUGGESTIONS);
        suggestions.add("-s");
        suggestions.add("-sender=");
        suggestions.add("-scope=");
        return filterSuggestions(suggestions, currentArg);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.ban");
    }

    private List<String> getOnlinePlayerNames() {
        return proxyServer.getAllPlayers().stream()
                .map(Player::getUsername)
                .collect(Collectors.toList());
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .collect(Collectors.toList());
    }
}