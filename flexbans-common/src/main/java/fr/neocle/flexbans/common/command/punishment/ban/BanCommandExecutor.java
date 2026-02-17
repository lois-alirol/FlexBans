package fr.neocle.flexbans.common.command.punishment.ban;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.api.punishment.BanExecutor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BanCommandExecutor implements ICommandExecutor {
    private final BanExecutor banExecutor;
    private final IBanCommandHelper helper;

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

    public BanCommandExecutor(BanExecutor banExecutor, IBanCommandHelper helper) {
        this.banExecutor = banExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String label = invocation.getLabel();
        String[] args = invocation.getArguments();

        if (args.length < 1) {
            if (source.isPlayer()) {
                helper.sendDialogMessage(source. getPlayerName(), "ban");
                return;
            }

            source.sendMessage(Component.text("Usage: /" + label + " <player/uuid> [duration] [reason] [-s] [-sender=<name>] [-scope=<server>]"));
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

        String origin = source.getOrigin();

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified. "));
            return;
        }

        if (sender == null || sender.isEmpty()) {
            sender = source.getName();
        }

        if (! scope.equalsIgnoreCase("Global") && !helper.isServerRegistered(scope)) {
            source.sendMessage(Component.text("§cError: The specified server '" + scope + "' is not registered."));
            return;
        }

        String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);
        boolean isIpBan = label.contains("ban") && label.contains("ip");

        banExecutor.executeBan(target, sender, duration, reason, scope, origin, silent, isIpBan,
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
            String partial = currentArg.substring(prefix.length());
            return helper.getOnlinePlayerNames().stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .toList();
        }

        if (currentArg.startsWith("-scope=")) {
            String prefix = "-scope=";
            String partial = currentArg.substring(prefix.length());
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
            suggestions. addAll(REASON_SUGGESTIONS);
            suggestions.add("-s");
            suggestions.add("-sender=");
            suggestions.add("-scope=");
            return filterSuggestions(suggestions, currentArg);
        }

        if (args.length >= 2 && !hasDuration && !hasFlag) {
            List<String> suggestions = new ArrayList<>(DURATION_SUGGESTIONS);
            suggestions. addAll(REASON_SUGGESTIONS);
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

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .toList();
    }
}