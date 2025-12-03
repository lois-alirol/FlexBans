package fr.neocle.flexbans.common.command.punishment.mute;

import fr.neocle.flexbans.api.punishment.MuteExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class MuteCommandExecutor implements ICommandExecutor {
    private final MuteExecutor muteExecutor;
    private final IMuteCommandHelper helper;

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\d+(s|m|h|d|w|mo|y)$");

    public MuteCommandExecutor(MuteExecutor muteExecutor, IMuteCommandHelper helper) {
        this.muteExecutor = muteExecutor;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (args. length < 1) {
            if (source.isPlayer()) {
                helper.sendDialogMessage(source. getPlayerName(), "mute");
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

        String origin = source.getOrigin();

        if (target == null) {
            source.sendMessage(Component.text("Error: No player specified."));
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

        muteExecutor.executeMute(target, sender, duration, reason, scope, origin, silent, false,
                message -> source.sendMessage(Component.text(message)));
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }
}