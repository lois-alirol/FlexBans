package fr.neocle.flexbans.common.command.lookup.alt;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AltCommandExecutor implements ICommandExecutor {
    private final ProfilesManager profilesManager;
    private final PunishmentsManager punishmentsManager;
    private final IAltCommandHelper helper;

    public AltCommandExecutor(ProfilesManager profilesManager, PunishmentsManager punishmentsManager, IAltCommandHelper helper) {
        this.profilesManager = profilesManager;
        this.punishmentsManager = punishmentsManager;
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
            String playerName;
            try {
                playerName = profilesManager.getCurrentUsername(playerUuid).join();
            } catch (Exception e) {
                playerName = null;
            }
            if (playerName == null) playerName = "(Unknown)";

            NamedTextColor color;
            String status;

            boolean ipBanned = false;
            boolean playerBanned = false;
            try {
                ipBanned = punishmentsManager
                        .isIpPunished(PunishmentsManager.PunishmentType.BAN, ip, origin)
                        .join();
                playerBanned = punishmentsManager
                        .isPlayerPunished(PunishmentsManager.PunishmentType.BAN, playerUuid, origin)
                        .join();
            } catch (Exception ignored) {
            }

            if (ipBanned) {
                color = NamedTextColor.RED;
                status = "[IP BANNED]";
            } else if (playerBanned) {
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