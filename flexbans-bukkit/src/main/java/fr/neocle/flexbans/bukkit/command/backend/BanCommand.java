package fr.neocle.flexbans.bukkit.command.backend;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BanCommand {
    private final YamlConfiguration config;

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
            "Spam", "Advertising", "Exploiting", "Inappropriate_behavior",
            "Harassment", "Racism", "Threats"
    );

    public BanCommand(YamlConfiguration config) {
        this.config = config;
    }

    public LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("ban")
                .requires(source -> source.getSender().hasPermission("flexbans.ban"))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Component.text("Usage: /ban <player/uuid> [duration] [reason] [-s] [-sender=<name>] [-scope=<server>]"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String currentArg = ctx.getArgument("args", String.class);
                            String[] splitArgs = currentArg.split(" ");
                            String lastArg = splitArgs[splitArgs.length - 1];
                            List<String> suggestions = getSuggestions(splitArgs);

                            for (String s : suggestions) {
                                builder.suggest(s);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String argsString = ctx.getArgument("args", String.class);
                            String[] args = argsString.split(" ");
                            CommandSourceStack source = ctx.getSource();

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
                                } else if (DURATION_PATTERN.matcher(arg).matches() && duration == null) {
                                    duration = arg;
                                } else if (target == null) {
                                    target = arg;
                                } else {
                                    reasonParts.add(arg);
                                }
                            }

                            if (target == null) {
                                source.getSender().sendMessage(Component.text("§cError: No player specified."));
                                return Command.SINGLE_SUCCESS;
                            }

                            if (sender == null || sender.isEmpty()) {
                                sender = source.getSender().getName();
                            }

                            String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);
                            String origin = config.getString("server-name", "Unknown");
                            boolean isIpBan = false;

                            // TODO: send to proxy the request
                            String reasonText = reason.isEmpty() ? "No reason provided" : reason;

                            StringBuilder msg = new StringBuilder();
                            msg.append("§cBanned player: §6").append(target).append("\n");
                            msg.append("§cReason: §e").append(reasonText).append("\n");
                            msg.append("§cBy: §b").append(sender).append("\n");
                            msg.append("§cServer: §a").append(origin).append("\n");
                            msg.append("§cScope: §d").append(scope).append("\n");
                            msg.append("§cSilent: §7").append(silent);

                            source.getSender().sendMessage(msg.toString());
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    private List<String> getSuggestions(String[] args) {
        String currentArg = args[args.length - 1].toLowerCase();

        if (args.length <= 1) {
            return filterSuggestions(getOnlinePlayerNames(), currentArg);
        }

        if (currentArg.startsWith("-sender=")) {
            String prefix = "-sender=";
            return getOnlinePlayerNames().stream()
                    .map(name -> prefix + name)
                    .filter(s -> s.toLowerCase().startsWith(currentArg))
                    .collect(Collectors.toList());
        }

        if (currentArg.startsWith("-scope=")) {
            return filterSuggestions(List.of("-scope=Global"), currentArg);
        }

        if (currentArg.startsWith("-")) {
            return filterSuggestions(List.of("-s", "-sender=", "-scope="), currentArg);
        }

        boolean hasDuration = false;
        boolean hasFlag = false;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("-")) hasFlag = true;
            if (DURATION_PATTERN.matcher(args[i]).matches()) hasDuration = true;
        }

        List<String> suggestions = new ArrayList<>();
        if (!hasDuration && !hasFlag && args.length == 2) {
            suggestions.addAll(DURATION_SUGGESTIONS);
        }

        suggestions.addAll(REASON_SUGGESTIONS);
        suggestions.add("-s");
        suggestions.add("-sender=");
        suggestions.add("-scope=");

        return filterSuggestions(suggestions, currentArg);
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .sorted()
                .collect(Collectors.toList());
    }

    public static void register(Commands commands, YamlConfiguration config) {
        BanCommand command = new BanCommand(config);
        commands.register(
                command.createNode().build(),
                "Ban a player from the server",
                List.of("ban", "fb:ban")
        );
    }
}