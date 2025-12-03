package fr.neocle.flexbans.common.command.punishment.kick;

import fr.neocle.flexbans.api.punishment.KickExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class KickCommandExecutor implements ICommandExecutor {
    private final KickExecutor kickExecutor;
    private final IKickCommandHelper helper;

    private static final List<String> REASON_SUGGESTIONS = List.of(
            "Spamming", "Disruptive behavior", "Inappropriate language",
            "Griefing", "PvP abuse", "AFK", "Server maintenance",
            "Lag", "Rule violation", "Staff request"
    );

    public KickCommandExecutor(KickExecutor kickExecutor, IKickCommandHelper helper) {
        this. kickExecutor = kickExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (args. length < 1) {
            if (source. isPlayer()) {
                helper.sendDialogMessage(source. getPlayerName(), "kick");
                return;
            }

            source.sendMessage(Component. text("Usage: /kick <player/uuid> [reason] [-s] [-sender=<name>]"));
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

        String origin = source.getOrigin();

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified."));
            return;
        }

        if (sender == null || sender. isEmpty()) {
            sender = source.getName();
        }

        String reason = reasonParts.isEmpty() ?  "" : String.join(" ", reasonParts);

        kickExecutor.executeKick(target, sender, reason, origin, silent, false,
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

        if (currentArg.startsWith("-")) {
            List<String> flags = new ArrayList<>();
            flags.add("-s");
            flags.add("-sender=");
            return filterSuggestions(flags, currentArg);
        }

        List<String> suggestions = new ArrayList<>(REASON_SUGGESTIONS);
        suggestions.add("-s");
        suggestions.add("-sender=");
        return filterSuggestions(suggestions, currentArg);
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .toList();
    }
}