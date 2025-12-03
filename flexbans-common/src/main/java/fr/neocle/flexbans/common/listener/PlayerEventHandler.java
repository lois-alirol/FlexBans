package fr.neocle.flexbans.common.listener;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.common.adapter.IPlatform;
import fr.neocle.flexbans.common.adapter.IPlayer;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.punishment.BansManager;
import fr.neocle.flexbans.database.punishment.HistoryManager;
import fr.neocle.flexbans.database.punishment.MutesManager;
import fr.neocle.flexbans.database.server.ServerLocksManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.DateCalculator;
import net.kyori.adventure.text.Component;
import net.kyori. adventure.text.minimessage.MiniMessage;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class PlayerEventHandler {
    private final Bootstrap bootstrap;
    private final IPlatform platformAdapter;
    private final BansManager bansManager;
    private final MutesManager mutesManager;
    private final ServerLocksManager serverLocksManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerEventHandler(Bootstrap bootstrap, IPlatform platformAdapter) {
        this.bootstrap = bootstrap;
        this.platformAdapter = platformAdapter;
        this. bansManager = bootstrap.getDatabaseUtils().getBansManager();
        this.mutesManager = bootstrap.getDatabaseUtils().getMutesManager();
        this.serverLocksManager = bootstrap.getDatabaseUtils().getServerLocksManager();
    }

    public void handlePlayerLogin(IPlayer player) {
        UUID playerUuid = player.getUniqueId();
        String playerIp = player.getIp();

        HistoryManager historyManager = bootstrap.getDatabaseUtils().getHistoryManager();

        if (! historyManager.playerExists(playerUuid)) {
            historyManager.insertPlayerData(playerUuid, player.getUsername(), playerIp);
        }
    }

    public void handlePlayerServerConnect(IPlayer player, String targetServer) {
        UUID targetUUID = player.getUniqueId();
        String playerIp = player.getIp();

        String rawBanMessage = LanguageManager. getMessageString("punishments.ban.disconnect-message");
        String rawLockMessage = LanguageManager. getMessageString("server-locks.disconnect-message");

        if (bansManager.isPlayerBanned(targetUUID, null)) {
            Component formattedBanMessage = formatBanMessage(rawBanMessage, targetUUID, null);
            player.disconnect(formattedBanMessage);
            return;
        }

        if (bansManager.isPlayerBanned(targetUUID, targetServer) ||
                bansManager.isIpBanned(playerIp, targetServer)) {
            Component formattedBanMessage = formatBanMessage(rawBanMessage, targetUUID, targetServer);

            if (player.getCurrentServer().isEmpty()) {
                player.disconnect(formattedBanMessage);
            } else {
                player.sendMessage(formattedBanMessage);
            }
            return;
        }

        if (!player.hasPermission("flexbans.serverlock.bypass")) {
            if (serverLocksManager.isServerLocked("Global")) {
                Component formattedLockMessage = formatLockMessage(rawLockMessage, "Global");
                player.disconnect(formattedLockMessage);
                return;
            }

            if (serverLocksManager. isServerLocked(targetServer)) {
                Component formattedLockMessage = formatLockMessage(rawLockMessage, targetServer);

                if (player.getCurrentServer(). isEmpty()) {
                    player. disconnect(formattedLockMessage);
                } else {
                    player.sendMessage(formattedLockMessage);
                }
            }
        }
    }

    public void handlePlayerChat(IPlayer player) {
        UUID playerUUID = player.getUniqueId();
        String serverName = player.getCurrentServer().orElse(null);
        String playerIp = player.getIp();

        if (isMuted(playerUUID, playerIp, serverName)) {
            Component formattedMuteMessage = formatMuteMessage(playerUUID, serverName);
            player.sendMessage(formattedMuteMessage);
            sendMuteSignal(player, true, serverName);
        }
    }

    public void handleCommandExecute(IPlayer player, String command) {
        UUID playerUUID = player.getUniqueId();
        String playerIp = player.getIp();
        String serverName = player.getCurrentServer().orElse(null);

        if (! isMuted(playerUUID, playerIp, serverName)) {
            return;
        }

        boolean blockAllCommands = ConfigManager.getBoolean("punishments-system.built-in.mutes.block-all-commands");
        String baseCommand = command.toLowerCase().trim().split(" ")[0];

        boolean shouldBlock = blockAllCommands;

        if (!blockAllCommands) {
            List<String> blockedCommands = ConfigManager. getList("punishments-system.built-in.mutes.blocked-commands");
            shouldBlock = blockedCommands.stream().anyMatch(cmd -> cmd.equalsIgnoreCase(baseCommand));
        }

        if (shouldBlock) {
            Component formattedMuteMessage = formatMuteMessage(playerUUID, serverName);
            player.sendMessage(formattedMuteMessage);
        }
    }

    public void handleMuteQueryMessage(UUID playerUuid, IPlayer sourcePlayer) throws IOException {
        String serverName = sourcePlayer.getCurrentServer(). orElse(null);
        boolean isMuted = isMuted(playerUuid, sourcePlayer.getIp(), serverName);
        String reason = isMuted ? mutesManager.getReason(playerUuid, serverName) : "";
        long until = isMuted ? mutesManager.getExpiration(playerUuid, serverName) : 0L;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(playerUuid.toString());
        out.writeBoolean(isMuted);
        out. writeUTF(reason != null ? reason : "");
        out.writeLong(until);
        out.writeUTF(serverName != null ? serverName : "Global");

        byte[] responseData = baos.toByteArray();
        platformAdapter.sendPluginMessage(sourcePlayer, "muting:response", responseData);
    }

    private boolean isMuted(UUID playerUUID, String playerIp, String serverName) {
        return mutesManager.isPlayerMuted(playerUUID, null) ||
                (serverName != null && mutesManager.isPlayerMuted(playerUUID, serverName)) ||
                mutesManager.isIpMuted(playerIp, null) ||
                (serverName != null && mutesManager. isIpMuted(playerIp, serverName));
    }

    private Component formatBanMessage(String rawMessage, UUID playerUUID, String serverName) {
        String reason = bansManager.getReason(playerUUID, serverName);
        String moderator = bansManager.getIssuer(playerUUID, serverName);
        String executionDate = DateCalculator.formatTimestamp(bansManager.getTime(playerUUID, serverName));
        String duration = DateCalculator.formatDuration(bansManager.getDuration(playerUUID, serverName));
        String endDate = bansManager.getDuration(playerUUID, serverName) <= 0 ? "Never" :
                DateCalculator.formatTimestamp(bansManager.getTime(playerUUID, serverName) + bansManager.getDuration(playerUUID, serverName));
        String timeLeft = DateCalculator.formatExpiration(bansManager.getTime(playerUUID, serverName),
                bansManager.getDuration(playerUUID, serverName));

        String formattedMessage = rawMessage
                .replace("%reason%", reason != null ? reason : "No reason specified")
                .replace("%moderator%", moderator != null ? moderator : "Console")
                .replace("%date%", executionDate != null ? executionDate : "Unknown")
                .replace("%duration%", duration != null ? duration : "Permanent")
                .replace("%expiration-date%", endDate != null ? endDate : "Unknown")
                .replace("%time-left%", timeLeft != null ?  timeLeft : "Permanent");

        String cleanedMessage = formattedMessage.replaceFirst("(?s)\\n\\s*\\z", "");
        return miniMessage.deserialize(cleanedMessage);
    }

    private Component formatLockMessage(String rawMessage, String serverName) {
        String formattedMessage = rawMessage
                .replace("%server%", serverName)
                .replace("%reason%", serverLocksManager.getReason(serverName))
                .replace("%moderator%", serverLocksManager.getIssuer(serverName))
                .replace("%date%", DateCalculator. formatTimestamp(serverLocksManager.getTime(serverName)));

        String cleanedMessage = formattedMessage.replaceFirst("(?s)\\n\\s*\\z", "");
        return miniMessage.deserialize(cleanedMessage);
    }

    private Component formatMuteMessage(UUID playerUUID, String serverName) {
        String reason = mutesManager.getReason(playerUUID, serverName);
        String issuer = mutesManager.getIssuer(playerUUID, serverName);
        long time = mutesManager.getTime(playerUUID, serverName);
        long duration = mutesManager.getDuration(playerUUID, serverName);

        String rawMuteMessage = LanguageManager.getMessageString("punishments.mute.chat-message");
        if (rawMuteMessage == null || rawMuteMessage.isEmpty()) {
            rawMuteMessage = "§cYou are muted and cannot chat.";
        } else {
            rawMuteMessage = rawMuteMessage
                    . replace("%reason%", reason != null ? reason : "No reason specified")
                    .replace("%moderator%", issuer != null ?  issuer : "Console")
                    .replace("%date%", DateCalculator.formatTimestamp(time))
                    .replace("%duration%", DateCalculator.formatDuration(duration))
                    . replace("%expiration-date%", duration <= 0 ? "Never" : DateCalculator.formatTimestamp(time + duration))
                    .replace("%time-left%", DateCalculator.formatExpiration(time, duration));
        }

        String cleanedMessage = rawMuteMessage.replaceFirst("(?s)\\n\\s*\\z", "");
        return miniMessage.deserialize(cleanedMessage);
    }

    private void sendMuteSignal(IPlayer player, boolean isMuted, String serverName) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);

        try {
            out.writeUTF(player.getUniqueId().toString());
            out. writeBoolean(isMuted);
            out.writeUTF(serverName);
            platformAdapter.sendPluginMessage(player, "muting:channel", byteStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}