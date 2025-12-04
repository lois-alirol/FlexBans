package fr.neocle.flexbans.common.command.lookup.alt;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr. neocle.flexbans.database.punishment.BansManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AltCommandExecutor implements ICommandExecutor {
    private final ProfilesManager profilesManager;
    private final BansManager bansManager;
    private final IAltCommandHelper helper;

    public AltCommandExecutor(ProfilesManager profilesManager, BansManager bansManager, IAltCommandHelper helper) {
        this.profilesManager = profilesManager;
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
        InetAddress inetAddress = helper.getPlayerInetAddress(playerName);

        if (inetAddress == null) {
            source.sendMessage(Component.text("Player not found in database.").color(NamedTextColor.RED));
            return;
        }

        displayPlayersWithSameIP(source, inetAddress);
    }

    private void displayPlayersWithSameIP(ICommandSource source, InetAddress ip) {
        source.sendMessage(
                Component.text("Players connected from IP: " + ip)
                        .color(NamedTextColor.YELLOW)
                        .decoration(TextDecoration.BOLD, true)
        );

        List<UUID> playersWithSameIP = profilesManager.getPlayersByIp(ip);

        if (playersWithSameIP.isEmpty()) {
            source.sendMessage(Component.text("No players found with this IP.").color(NamedTextColor.RED));
            return;
        }

        String origin = source.getOrigin();

        for (UUID playerUuid : playersWithSameIP) {
            String playerName = profilesManager.getCurrentUsername(playerUuid);
            if (playerName == null) playerName = "(Unknown)";

            NamedTextColor color;
            String status;

            if (bansManager.isIpBanned(ip, origin)) {
                color = NamedTextColor.RED;
                status = "[IP BANNED]";
            } else if (bansManager.isPlayerBanned(playerUuid, origin)) {
                color = NamedTextColor.GOLD;
                status = "[BANNED]";
            } else if (helper.isPlayerOnline(playerUuid)) {
                color = NamedTextColor.GREEN;
                status = "[ONLINE]";
            } else {
                color = NamedTextColor.GRAY;
                status = "[OFFLINE]";
            }

            source.sendMessage(
                    Component.text(status + " " + playerName).color(color)
            );
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