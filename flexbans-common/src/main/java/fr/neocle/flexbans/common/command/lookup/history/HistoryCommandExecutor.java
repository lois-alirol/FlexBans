package fr.neocle.flexbans.common. command. lookup. history;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common. adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common. adapter.command.ICommandSource;
import fr.neocle. flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.database.punishment.BansManager;
import fr.neocle.flexbans.database. punishment.KicksManager;
import fr.neocle.flexbans.database.punishment.MutesManager;
import fr.neocle. flexbans.database.punishment. WarningsManager;
import net.kyori.adventure.text. Component;
import net.kyori.adventure.text.format. NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HistoryCommandExecutor implements ICommandExecutor {
    private final ProfilesManager profilesManager;
    private final BansManager bansManager;
    private final MutesManager mutesManager;
    private final WarningsManager warningsManager;
    private final KicksManager kicksManager;
    private final IHistoryCommandHelper helper;
    private static final int ENTRIES_PER_PAGE = 5;

    public HistoryCommandExecutor(ProfilesManager profilesManager, BansManager bansManager,
                                  MutesManager mutesManager, WarningsManager warningsManager,
                                  KicksManager kicksManager, IHistoryCommandHelper helper) {
        this.profilesManager = profilesManager;
        this. bansManager = bansManager;
        this.mutesManager = mutesManager;
        this. warningsManager = warningsManager;
        this.kicksManager = kicksManager;
        this. helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (! source.hasPermission("flexbans.command.history")) {
            source. sendMessage(Component.text("You don't have permission to use this command.")
                    .color(NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(Component.text("Usage: /history <player> [page] [-type=<type>]")
                    .color(NamedTextColor.RED));
            return;
        }

        String playerName = args[0];
        UUID playerUuid = helper.getPlayerUuid(playerName);

        if (playerUuid == null) {
            source.sendMessage(Component.text("Player not found in database.")
                    .color(NamedTextColor.RED));
            return;
        }

        int page = 1;
        String typeFilter = null;

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

        displayPaginatedHistory(source, playerUuid, playerName, page, typeFilter);
    }

    private boolean isValidType(String type) {
        return type.equals("ban") || type.equals("mute") || type.equals("warning") || type.equals("kick")
                || type.equals("ip-ban") || type.equals("ip-mute") || type. equals("ip-warning");
    }

    private void displayPaginatedHistory(ICommandSource source, UUID playerUuid, String playerName, int page, String typeFilter) {
        List<HistoryEntry> allEntries = helper.getPlayerHistory(playerUuid);

        // Filter by type if specified
        if (typeFilter != null) {
            allEntries = filterByType(allEntries, typeFilter);
        }

        if (allEntries.isEmpty()) {
            String typeInfo = typeFilter != null ? " with type " + typeFilter : "";
            source.sendMessage(Component. text("No punishment history found for " + playerName + typeInfo + ".")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        int totalPages = (int) Math.ceil((double) allEntries.size() / ENTRIES_PER_PAGE);

        if (page > totalPages) {
            source.sendMessage(Component. text("Page " + page + " does not exist.  Maximum page: " + totalPages)
                    .color(NamedTextColor.RED));
            return;
        }

        // Display header
        source.sendMessage(
                Component.text("═══════════════════════════════════════")
                        .color(NamedTextColor.GRAY)
        );
        source.sendMessage(
                Component.text("Punishment History for " + playerName)
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration. BOLD, true)
        );
        if (typeFilter != null) {
            source.sendMessage(
                    Component.text("Filter: " + typeFilter. toUpperCase())
                            . color(NamedTextColor. AQUA)
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

        // Calculate pagination indices
        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, allEntries.size());

        // Display entries for current page
        for (int i = startIndex; i < endIndex; i++) {
            HistoryEntry entry = allEntries. get(i);
            displayHistoryEntry(source, entry, i + 1);
        }

        // Display footer with navigation
        source.sendMessage(
                Component.text("═══════════════════════════════════════")
                        .color(NamedTextColor.GRAY)
        );

        if (page > 1 || page < totalPages) {
            Component navigationComponent = Component.empty();

            if (page > 1) {
                String command = "/history " + playerName + " " + (page - 1);
                if (typeFilter != null) {
                    command += " -type=" + typeFilter;
                }
                navigationComponent = navigationComponent.append(
                        Component.text("« Previous")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(net.kyori.adventure.text. event. ClickEvent.runCommand(command))
                                .hoverEvent(net.kyori. adventure.text.event.HoverEvent.showText(
                                        Component.text("Go to page " + (page - 1))
                                ))
                );
            }

            if (page > 1 && page < totalPages) {
                navigationComponent = navigationComponent.append(Component.text(" | ").color(NamedTextColor.GRAY));
            }

            if (page < totalPages) {
                String command = "/history " + playerName + " " + (page + 1);
                if (typeFilter != null) {
                    command += " -type=" + typeFilter;
                }
                navigationComponent = navigationComponent.append(
                        Component.text("Next »")
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

    private List<HistoryEntry> filterByType(List<HistoryEntry> entries, String typeFilter) {
        List<HistoryEntry> filtered = new ArrayList<>();

        for (HistoryEntry entry : entries) {
            String entryType = entry.type(). toLowerCase();

            if (typeFilter. equals("ban") && (entryType.equals("ban") || entryType.equals("ip-ban"))) {
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

    private void displayHistoryEntry(ICommandSource source, HistoryEntry entry, int index) {
        NamedTextColor typeColor = getColorForType(entry.type());
        String typePrefix = getPrefixForType(entry.type());

        Component entryComponent = Component.text(index + ". ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(typePrefix)
                        . color(typeColor)
                        .decoration(TextDecoration. BOLD, true))
                .append(Component.text(" | ")
                        .color(NamedTextColor.GRAY))
                .append(Component.text("Issued by: " + entry.issuerName())
                        .color(NamedTextColor.WHITE))
                .append(Component. text(" | ")
                        . color(NamedTextColor. GRAY))
                .append(Component.text(helper.formatTime(entry.time()))
                        .color(NamedTextColor.DARK_GRAY));

        source.sendMessage(entryComponent);

        Component reasonComponent = Component.text("   └─ Reason: ")
                .color(NamedTextColor. GRAY)
                .append(Component.text(entry.reason())
                        .color(NamedTextColor.WHITE)
                        .hoverEvent(net.kyori. adventure. text.event.HoverEvent. showText(
                                Component. text(entry.reason())
                        )));

        source.sendMessage(reasonComponent);

        if (! entry.status().equals("active")) {
            Component statusComponent = Component.text("   └─ Status: ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(entry.status(). toUpperCase())
                            . color(getColorForStatus(entry.status())));

            if (entry.removalReason() != null && !entry.removalReason(). isEmpty()) {
                statusComponent = statusComponent.append(Component.text(" - " + entry.removalReason())
                        .color(NamedTextColor.WHITE));
            }

            source. sendMessage(statusComponent);
        }

        source.sendMessage(Component.empty());
    }

    private NamedTextColor getColorForType(String type) {
        return switch (type. toLowerCase()) {
            case "ban" -> NamedTextColor. RED;
            case "ip-ban" -> NamedTextColor. DARK_RED;
            case "mute" -> NamedTextColor.GOLD;
            case "ip-mute" -> NamedTextColor.DARK_AQUA;
            case "warning" -> NamedTextColor. YELLOW;
            case "ip-warning" -> NamedTextColor.LIGHT_PURPLE;
            case "kick" -> NamedTextColor.LIGHT_PURPLE;
            default -> NamedTextColor.GRAY;
        };
    }

    private String getPrefixForType(String type) {
        return switch (type. toLowerCase()) {
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
            case "expired" -> NamedTextColor.DARK_GRAY;
            case "removed" -> NamedTextColor. GREEN;
            case "active" -> NamedTextColor.RED;
            default -> NamedTextColor.GRAY;
        };
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation.getArguments();

        if (args. length == 1) {
            String partialName = args[0]. toLowerCase();
            return helper.getOnlinePlayerSuggestions(partialName);
        }

        if (args.length > 1 && args[args.length - 1].startsWith("-type=")) {
            String partial = args[args.length - 1]. substring(6).toLowerCase();
            List<String> suggestions = new ArrayList<>();

            String[] types = {"ban", "mute", "warning", "kick", "ip-ban", "ip-mute", "ip-warning"};
            for (String type : types) {
                if (type.startsWith(partial)) {
                    suggestions.add("-type=" + type);
                }
            }

            return suggestions;
        }

        return Collections. emptyList();
    }
}