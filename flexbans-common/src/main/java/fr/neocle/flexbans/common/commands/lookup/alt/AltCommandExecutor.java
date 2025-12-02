package fr.neocle.flexbans.common.commands.lookup.alt;

import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr. neocle.flexbans.database.punishments.BansManager;
import fr.neocle.flexbans.database.punishments.HistoryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AltCommandExecutor implements ICommandExecutor {
    private final HistoryManager historyManager;
    private final BansManager bansManager;
    private final IAltCommandHelper helper;

    public AltCommandExecutor(HistoryManager historyManager, BansManager bansManager, IAltCommandHelper helper) {
        this.historyManager = historyManager;
        this.bansManager = bansManager;
        this.helper = helper;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (! source.hasPermission("flexbans.command.alt")) {
            source. sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            source. sendMessage(Component.text("Usage: /alt <player>").color(NamedTextColor.RED));
            return;
        }

        String playerName = args[0];
        String ip = helper.getPlayerIP(playerName);

        if (ip == null) {
            source.sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED));
            return;
        }

        displayPlayersWithSameIP(source, ip);
    }

    private void displayPlayersWithSameIP(ICommandSource source, String ip) {
        source.sendMessage(Component.text("Players connected from IP: " + ip).color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.BOLD, true));

        List<String[]> playersWithSameIP = historyManager.getPlayersWithSameIP(ip);

        if (playersWithSameIP.isEmpty()) {
            source.sendMessage(Component.text("No players found with this IP."). color(NamedTextColor. RED));
            return;
        }

        String origin = source.getOrigin();

        for (String[] playerInfo : playersWithSameIP) {
            String playerUuid = playerInfo[0];
            String playerName = playerInfo[1];
            String playerIp = playerInfo[2];

            NamedTextColor color;
            String status;

            if (bansManager.isIpBanned(playerIp, origin)) {
                color = NamedTextColor. RED;
                status = "[IP BANNED]";
            } else if (bansManager.isPlayerBanned(UUID.fromString(playerUuid), origin)) {
                color = NamedTextColor.GOLD;
                status = "[BANNED]";
            } else if (helper.isPlayerOnline(UUID. fromString(playerUuid))) {
                color = NamedTextColor.GREEN;
                status = "[ONLINE]";
            } else {
                color = NamedTextColor.GRAY;
                status = "[OFFLINE]";
            }

            source. sendMessage(Component.text(status + " " + playerName).color(color));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        String[] args = invocation. getArguments();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return helper.getOnlinePlayerSuggestions(partialName);
        }

        return Collections. emptyList();
    }
}