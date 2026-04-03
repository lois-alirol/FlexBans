package fr. neocle.flexbans. common.command.lookup.moderatorhistory;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common. adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.player.ProfilesManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format. NamedTextColor;
import net.kyori.adventure.text. format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ModeratorHistoryCommandExecutor implements ICommandExecutor {
    private final IModeratorHistoryCommandHelper helper;
    private static final int ENTRIES_PER_PAGE = 5;

    public ModeratorHistoryCommandExecutor(IModeratorHistoryCommandHelper helper) {
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (!source. hasPermission("flexbans. command.moderatorhistory")) {
            source.sendMessage(Component.text("You don't have permission to use this command.")
                    .color(NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /modhistory <moderator> [page] [-type=<type>] [-action=<action>]")
                    .color(NamedTextColor.RED));
            return;
        }

        String moderatorName = args[0];
        UUID moderatorUuid = helper.getModeratorUuid(moderatorName);

        if (moderatorUuid == null) {
            source.sendMessage(Component.text("Moderator not found in database.")
                    .color(NamedTextColor.RED));
            return;
        }

        int page = 1;
        String typeFilter = null;
        String actionFilter = null;

        // Parse arguments
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("-type=")) {
                typeFilter = arg.substring(6). toLowerCase();
                if (! isValidType(typeFilter)) {
                    source.sendMessage(Component.text("Invalid type.  Valid types: ban, mute, warning, kick, ip-ban, ip-mute, ip-warning")
                            .color(NamedTextColor.RED));
                    return;
                }
            } else if (arg.startsWith("-action=")) {
                actionFilter = arg.substring(8). toLowerCase();
                if (!isValidAction(actionFilter)) {
                    source.sendMessage(Component. text("Invalid action. Valid actions: issued, removed")
                            . color(NamedTextColor. RED));
                    return;
                }
            } else {
                try {
                    page = Integer. parseInt(arg);
                    if (page < 1) {
                        source.sendMessage(Component.text("Page number must be greater than 0.")
                                . color(NamedTextColor. RED));
                        return;
                    }
                } catch (NumberFormatException e) {
                    source.sendMessage(Component.text("Invalid page number.")
                            .color(NamedTextColor.RED));
                    return;
                }
            }
        }

        displayPaginatedModeratorHistory(source, moderatorUuid, moderatorName, page, typeFilter, actionFilter);
    }

    private boolean isValidType(String type) {
        return type.equals("ban") || type.equals("mute") || type.equals("warning") || type.equals("kick")
                || type.equals("ip-ban") || type.equals("ip-mute") || type. equals("ip-warning");
    }

    private boolean isValidAction(String action) {
        return action.equals("issued") || action.equals("removed");
    }

    private void displayPaginatedModeratorHistory(ICommandSource source, UUID moderatorUuid, String moderatorName,
                                                  int page, String typeFilter, String actionFilter) {
        List<ProfilesManager.ModeratorHistoryEntry> allEntries = helper.getModeratorHistory(moderatorUuid);

        if (actionFilter != null) {
            allEntries = filterByAction(allEntries, actionFilter);
        }

        if (typeFilter != null) {
            allEntries = filterByType(allEntries, typeFilter);
        }

        if (allEntries.isEmpty()) {
            String filterInfo = "";
            if (actionFilter != null) filterInfo += " with action " + actionFilter;
            if (typeFilter != null) filterInfo += " with type " + typeFilter;
            source.sendMessage(Component.text("No moderation history found for " + moderatorName + filterInfo + ".")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        int totalPages = (int) Math.ceil((double) allEntries.size() / ENTRIES_PER_PAGE);

        if (page > totalPages) {
            source.sendMessage(Component.text("Page " + page + " does not exist.  Maximum page: " + totalPages)
                    .color(NamedTextColor.RED));
            return;
        }

        // Display header
        source.sendMessage(
                Component.text("═══════════════════════════════════════")
                        .color(NamedTextColor.GRAY)
        );
        source.sendMessage(
                Component.text("Moderation History for " + moderatorName)
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration. BOLD, true)
        );
        if (actionFilter != null) {
            source.sendMessage(
                    Component.text("Filter: " + actionFilter. toUpperCase())
                            . color(NamedTextColor. AQUA)
            );
        }
        if (typeFilter != null) {
            source.sendMessage(
                    Component.text("Type: " + typeFilter.toUpperCase())
                            .color(NamedTextColor. AQUA)
            );
        }
        source.sendMessage(
                Component.text("Page " + page + " of " + totalPages + " (" + allEntries.size() + " total)")
                        .color(NamedTextColor.GRAY)
        );
        source.sendMessage(
                Component.text("═══════════════════════════════════════")
                        .color(NamedTextColor.GRAY)
        );

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, allEntries.size());

        for (int i = startIndex; i < endIndex; i++) {
            ProfilesManager.ModeratorHistoryEntry entry = allEntries.get(i);
            displayModeratorHistoryEntry(source, entry, i + 1);
        }

        source.sendMessage(
                Component.text("═══════════════════════════════════════")
                        .color(NamedTextColor.GRAY)
        );

        if (page > 1 || page < totalPages) {
            Component navigationComponent = Component.empty();

            if (page > 1) {
                String command = "/modhistory " + moderatorName + " " + (page - 1);
                if (actionFilter != null) {
                    command += " -action=" + actionFilter;
                }
                if (typeFilter != null) {
                    command += " -type=" + typeFilter;
                }
                navigationComponent = navigationComponent.append(
                        Component.text("« Previous")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(net.kyori.adventure.text.event. ClickEvent.runCommand(command))
                                .hoverEvent(net.kyori. adventure.text.event.HoverEvent.showText(
                                        Component.text("Go to page " + (page - 1))
                                ))
                );
            }

            if (page > 1 && page < totalPages) {
                navigationComponent = navigationComponent.append(Component.text(" | "). color(NamedTextColor. GRAY));
            }

            if (page < totalPages) {
                String command = "/modhistory " + moderatorName + " " + (page + 1);
                if (actionFilter != null) {
                    command += " -action=" + actionFilter;
                }
                if (typeFilter != null) {
                    command += " -type=" + typeFilter;
                }
                navigationComponent = navigationComponent.append(
                        Component. text("Next »")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command))
                                . hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                        Component.text("Go to page " + (page + 1))
                                ))
                );
            }

            source.sendMessage(navigationComponent);
        }
    }

    private List<ProfilesManager.ModeratorHistoryEntry> filterByAction(List<ProfilesManager.ModeratorHistoryEntry> entries, String actionFilter) {
        List<ProfilesManager.ModeratorHistoryEntry> filtered = new ArrayList<>();

        for (ProfilesManager.ModeratorHistoryEntry entry : entries) {
            if (entry.action(). toLowerCase().equals(actionFilter)) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    private List<ProfilesManager.ModeratorHistoryEntry> filterByType(List<ProfilesManager.ModeratorHistoryEntry> entries, String typeFilter) {
        List<ProfilesManager.ModeratorHistoryEntry> filtered = new ArrayList<>();

        for (ProfilesManager.ModeratorHistoryEntry entry : entries) {
            String entryType = entry.type(). toLowerCase();

            if (typeFilter.equals("ban") && (entryType.equals("ban") || entryType.equals("ip-ban"))) {
                filtered.add(entry);
            } else if (typeFilter.equals("mute") && (entryType.equals("mute") || entryType.equals("ip-mute"))) {
                filtered. add(entry);
            } else if (typeFilter.equals("warning") && (entryType.equals("warning") || entryType. equals("ip-warning"))) {
                filtered.add(entry);
            } else if (typeFilter.equals("kick") && entryType.equals("kick")) {
                filtered.add(entry);
            } else if (typeFilter.equals("ip-ban") && entryType.equals("ip-ban")) {
                filtered.add(entry);
            } else if (typeFilter.equals("ip-mute") && entryType.equals("ip-mute")) {
                filtered.add(entry);
            } else if (typeFilter.equals("ip-warning") && entryType.equals("ip-warning")) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    private void displayModeratorHistoryEntry(ICommandSource source, ProfilesManager.ModeratorHistoryEntry entry, int index) {
        NamedTextColor actionColor = getColorForAction(entry.action());
        NamedTextColor typeColor = getColorForType(entry. type());
        String typePrefix = getPrefixForType(entry.type());
        String actionPrefix = getPrefixForAction(entry.action());

        Component entryComponent = Component.text(index + ".  ")
                .color(NamedTextColor. GRAY)
                .append(Component.text(actionPrefix)
                        .color(actionColor)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ")
                        .color(NamedTextColor.GRAY))
                .append(Component.text(typePrefix)
                        .color(typeColor))
                .append(Component.text(" | ")
                        .color(NamedTextColor.GRAY))
                .append(Component.text("Target: " + entry.targetName())
                        .color(NamedTextColor.WHITE))
                .append(Component. text(" | ")
                        . color(NamedTextColor. GRAY))
                .append(Component.text(helper.formatTime(entry.timestamp()))
                        .color(NamedTextColor.DARK_GRAY));

        source. sendMessage(entryComponent);

        Component reasonComponent = Component.text("   └─ Reason: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(entry.reason())
                        .color(NamedTextColor.WHITE)
                        .hoverEvent(net.kyori. adventure.text.event.HoverEvent.showText(
                                Component.text(entry.reason())
                        )));

        source.sendMessage(reasonComponent);

        if (entry. duration() > 0 && entry.action().equals("issued")) {
            Component durationComponent = Component.text("   └─ Duration: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(formatDuration(entry.duration()))
                            .color(NamedTextColor.WHITE));

            source.sendMessage(durationComponent);
        }

        if (! entry.status().equals("active") && entry.action().equals("issued")) {
            Component statusComponent = Component.text("   └─ Status: ")
                    .color(NamedTextColor. GRAY)
                    .append(Component.text(entry.status(). toUpperCase())
                            . color(getColorForStatus(entry.status())));

            source.sendMessage(statusComponent);
        }

        source.sendMessage(Component.empty());
    }

    private String formatDuration(long duration) {
        if (duration == 0) return "Permanent";

        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    private NamedTextColor getColorForAction(String action) {
        return switch (action. toLowerCase()) {
            case "issued" -> NamedTextColor. RED;
            case "removed" -> NamedTextColor.GREEN;
            default -> NamedTextColor.GRAY;
        };
    }

    private String getPrefixForAction(String action) {
        return switch (action.toLowerCase()) {
            case "issued" -> "◆ ISSUED";
            case "removed" -> "◆ REMOVED";
            default -> "◆ UNKNOWN";
        };
    }

    private NamedTextColor getColorForType(String type) {
        return switch (type.toLowerCase()) {
            case "ban" -> NamedTextColor.RED;
            case "ip-ban" -> NamedTextColor. DARK_RED;
            case "mute" -> NamedTextColor.GOLD;
            case "ip-mute" -> NamedTextColor.DARK_AQUA;
            case "warning" -> NamedTextColor. YELLOW;
            case "ip-warning" -> NamedTextColor.LIGHT_PURPLE;
            case "kick" -> NamedTextColor. LIGHT_PURPLE;
            default -> NamedTextColor. GRAY;
        };
    }

    private String getPrefixForType(String type) {
        return switch (type.toLowerCase()) {
            case "ban" -> "● BAN";
            case "ip-ban" -> "● IP BAN";
            case "mute" -> "● MUTE";
            case "ip-mute" -> "● IP MUTE";
            case "warning" -> "● WARNING";
            case "ip-warning" -> "● IP WARNING";
            case "kick" -> "● KICK";
            default -> "● UNKNOWN";
        };
    }

    private NamedTextColor getColorForStatus(String status) {
        return switch (status.toLowerCase()) {
            case "expired" -> NamedTextColor. DARK_GRAY;
            case "removed" -> NamedTextColor. GREEN;
            case "active" -> NamedTextColor.RED;
            default -> NamedTextColor.GRAY;
        };
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();

        if (args.length == 1) {
            String partialName = args[0]. toLowerCase();
            return helper.getOnlineModerators(partialName);
        }

        if (args.length > 1 && args[args.length - 1].startsWith("-type=")) {
            String partial = args[args.length - 1]. substring(6). toLowerCase();
            List<String> suggestions = new ArrayList<>();

            String[] types = {"ban", "mute", "warning", "kick", "ip-ban", "ip-mute", "ip-warning"};
            for (String type : types) {
                if (type.startsWith(partial)) {
                    suggestions.add("-type=" + type);
                }
            }

            return suggestions;
        }

        if (args.length > 1 && args[args.length - 1].startsWith("-action=")) {
            String partial = args[args.length - 1].substring(8).toLowerCase();
            List<String> suggestions = new ArrayList<>();

            String[] actions = {"issued", "removed"};
            for (String action : actions) {
                if (action.startsWith(partial)) {
                    suggestions. add("-action=" + action);
                }
            }

            return suggestions;
        }

        return Collections.emptyList();
    }
}