package fr.neocle.flexbans.common.commands.server.lock;

import fr.neocle.flexbans.api.server.ServerLockExecutor;
import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr. neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ServerLockCommandExecutor implements ICommandExecutor {
    private final ServerLockExecutor serverLockExecutor;
    private final IServerLockCommandHelper helper;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+(s|m|h|d|w|mo|y)$");

    private static final List<String> DURATION_SUGGESTIONS = List.of(
            "1h", "2h", "6h", "12h",
            "1d", "2d", "3d", "7d", "14d", "30d",
            "1w", "2w", "4w",
            "1mo", "3mo", "6mo",
            "1y"
    );

    private static final List<String> REASON_SUGGESTIONS = List.of(
            "Maintenance", "Update", "Crash", "Restart required",
            "Emergency closure", "Server full", "Beta testing"
    );

    public ServerLockCommandExecutor(ServerLockExecutor serverLockExecutor, IServerLockCommandHelper helper) {
        this.serverLockExecutor = serverLockExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        String serverName = null;
        String sender = null;
        String duration = null;
        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.startsWith("-sender=")) {
                sender = arg.substring(8);
            } else if (serverName == null) {
                serverName = arg;
            } else if (DURATION_PATTERN.matcher(arg).matches() && (duration == null || duration.isEmpty())) {
                duration = arg;
            } else {
                reasonParts.add(arg);
            }
        }

        if (serverName == null || serverName.isEmpty()) {
            serverName = helper.getDefaultServerName(source);
        }

        if (!serverName.equalsIgnoreCase("Global") && ! helper.isServerRegistered(serverName)) {
            source.sendMessage(Component.text("§cError: The specified server '" + serverName + "' is not registered. "));
            return;
        }

        String origin = source.getOrigin();

        if (sender == null || sender.isEmpty()) {
            sender = source. getName();
        }

        String reason = reasonParts.isEmpty() ?  "No reason provided" : String. join(" ", reasonParts);

        serverLockExecutor.lockServer(serverName, sender, duration, reason, origin, silent,
                message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            String currentArg = args. length > 0 ? args[args.length - 1]. toLowerCase() : "";
            return filterSuggestions(helper.getRegisteredServers(), currentArg);
        }

        String currentArg = args[args.length - 1].toLowerCase();

        if (currentArg.startsWith("-sender=")) {
            String prefix = "-sender=";
            return helper.getOnlinePlayerNames(). stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .toList();
        }

        if (currentArg.startsWith("-")) {
            List<String> flags = new ArrayList<>();
            flags.add("-s");
            flags.add("-sender=");
            return filterSuggestions(flags, currentArg);
        }

        // Suggest durations, reasons, and flags
        List<String> allSuggestions = new ArrayList<>(DURATION_SUGGESTIONS);
        allSuggestions.addAll(REASON_SUGGESTIONS);
        allSuggestions. add("-s");
        allSuggestions.add("-sender=");
        return filterSuggestions(allSuggestions, currentArg);
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .toList();
    }
}