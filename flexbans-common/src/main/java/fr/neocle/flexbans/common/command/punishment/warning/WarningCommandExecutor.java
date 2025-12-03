package fr.neocle.flexbans.common.command.punishment.warning;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.command.punishment.warning.WarningExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class WarningCommandExecutor implements ICommandExecutor {
    private final WarningExecutor warningExecutor;
    private final IWarningCommandHelper helper;

    private static final List<String> REASON_SUGGESTIONS = List.of(
            "Spamming", "Disruptive behavior", "Inappropriate language",
            "Griefing", "PvP abuse", "Exploiting", "Rule violation",
            "First warning", "Second warning", "Final warning"
    );

    public WarningCommandExecutor(WarningExecutor warningExecutor, IWarningCommandHelper helper) {
        this.warningExecutor = warningExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /warn <player/uuid> [reason] [-s] [-sender=<name>] [-scope=<server>]"));
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
                sender = arg. substring(8);
            } else if (arg.startsWith("-scope=")) {
                scope = arg.substring(7);
            } else if (target == null) {
                target = arg;
            } else {
                reasonParts.add(arg);
            }
        }

        String origin = source.getOrigin();

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified."));
            return;
        }

        if (sender == null || sender. isEmpty()) {
            sender = source. getName();
        }

        if (! scope.equalsIgnoreCase("Global") && ! helper.isServerRegistered(scope)) {
            source.sendMessage(Component.text("§cError: The specified server '" + scope + "' is not registered. "));
            return;
        }

        String reason = reasonParts.isEmpty() ? "" : String. join(" ", reasonParts);

        warningExecutor.executeWarning(target, sender, reason, scope, origin, silent, false,
                message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();

        if (args.length == 0) {
            return helper.getOnlinePlayerNames();
        }

        String currentArg = args[args.length - 1]. toLowerCase();

        if (args.length == 1) {
            return filterSuggestions(helper.getOnlinePlayerNames(), currentArg);
        }

        if (currentArg.startsWith("-sender=")) {
            String prefix = "-sender=";
            return helper.getOnlinePlayerNames().stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .toList();
        }

        if (currentArg.startsWith("-scope=")) {
            String prefix = "-scope=";
            List<String> suggestions = new ArrayList<>();
            suggestions.add(prefix + "Global");
            helper.getRegisteredServers().stream()
                    .map(server -> prefix + server)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .forEach(suggestions::add);
            return suggestions;
        }

        if (currentArg.startsWith("-")) {
            List<String> flags = new ArrayList<>();
            flags. add("-s");
            flags.add("-sender=");
            flags.add("-scope=");
            return filterSuggestions(flags, currentArg);
        }

        // Suggest reasons and flags
        List<String> suggestions = new ArrayList<>(REASON_SUGGESTIONS);
        suggestions.add("-s");
        suggestions.add("-sender=");
        suggestions.add("-scope=");
        return filterSuggestions(suggestions, currentArg);
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .toList();
    }
}
