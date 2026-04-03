package fr.neocle.flexbans.common.listener;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.common.adapter.IPlatform;
import fr.neocle.flexbans.common.adapter.IPlayer;
import fr.neocle.flexbans.common.messaging.Channel;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.database.punishment.PunishmentsManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.DateCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class PlayerEventHandler {
    private final Bootstrap bootstrap;
    private final IPlatform platformAdapter;
    private final PunishmentsManager punishmentsManager;
    private final ProfilesManager profilesManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PlayerEventHandler(Bootstrap bootstrap, IPlatform platformAdapter) {
        this.bootstrap = bootstrap;
        this.platformAdapter = platformAdapter;
        this.punishmentsManager = bootstrap.getDatabaseUtils().getPunishmentsManager();
        this.profilesManager = bootstrap.getDatabaseUtils().getProfilesManager();
    }

    public void handlePlayerLogin(IPlayer player) {
        UUID playerUuid = player.getUniqueId();
        // async fire-and-forget
        profilesManager.recordPlayerLogin(playerUuid, player.getUsername(), player.getInetAddress());
    }

    public void handlePlayerServerConnect(IPlayer player, String targetServer) {
        UUID playerUuid = player.getUniqueId();
        InetAddress playerIp = player.getInetAddress();

        String rawBanMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message");
        String rawLockMessage = LanguageManager.getMessageString("server-locks.disconnect-message");

        // BAN on targetServer
        if (punishmentsManager.isPlayerPunished(
                PunishmentsManager.PunishmentType.BAN, playerUuid, targetServer
        ).join()) {
            Component msg = formatBanMessage(rawBanMessage, playerUuid, targetServer);
            if (player.getCurrentServer().isEmpty()) player.disconnect(msg);
            else player.sendMessage(msg);
            return;
        }

        if (punishmentsManager.isIpPunished(
                PunishmentsManager.PunishmentType.BAN, playerIp, targetServer
        ).join()) {
            List<UUID> playerUuids = profilesManager.getPlayersByIp(playerIp);
            UUID banOwner = !playerUuids.isEmpty() ? playerUuids.get(0) : playerUuid;
            Component msg = formatBanMessage(rawBanMessage, banOwner, targetServer);
            if (player.getCurrentServer().isEmpty()) player.disconnect(msg);
            else player.sendMessage(msg);
            return;
        }

        // Global bans
        if (punishmentsManager.isPlayerPunished(
                PunishmentsManager.PunishmentType.BAN, playerUuid, "Global"
        ).join()) {
            Component msg = formatBanMessage(rawBanMessage, playerUuid, "Global");
            player.disconnect(msg);
            return;
        }

        if (punishmentsManager.isIpPunished(
                PunishmentsManager.PunishmentType.BAN, playerIp, "Global"
        ).join()) {
            List<UUID> playerUuids = profilesManager.getPlayersByIp(playerIp);
            UUID banOwner = !playerUuids.isEmpty() ? playerUuids.get(0) : playerUuid;
            Component msg = formatBanMessage(rawBanMessage, banOwner, "Global");
            player.disconnect(msg);
            return;
        }

        // Server locks
        if (!player.hasPermission("flexbans.serverlock.bypass")) {
            if (bootstrap.getDatabaseUtils().getServerLocksManager()
                    .isServerLocked("Global").join()) {
                Component formattedLockMessage = formatLockMessage(rawLockMessage, "Global");
                player.disconnect(formattedLockMessage);
                return;
            }
            if (bootstrap.getDatabaseUtils().getServerLocksManager()
                    .isServerLocked(targetServer).join()) {
                Component formattedLockMessage = formatLockMessage(rawLockMessage, targetServer);
                if (player.getCurrentServer().isEmpty()) {
                    player.disconnect(formattedLockMessage);
                } else {
                    player.sendMessage(formattedLockMessage);
                }
            }
        }
    }

    public void handlePlayerChat(IPlayer player) {
        UUID playerUUID = player.getUniqueId();
        String serverName = player.getCurrentServer().orElse(null);
        InetAddress playerIp = player.getInetAddress();

        if (isMuted(playerUUID, playerIp, serverName)) {
            Component formattedMuteMessage = formatMuteMessage(playerUUID, serverName);
            player.sendMessage(formattedMuteMessage);
            sendMuteSignal(player, true, serverName);
        }
    }

    public void handleCommandExecute(IPlayer player, String command) {
        UUID playerUUID = player.getUniqueId();
        InetAddress playerIp = player.getInetAddress();
        String serverName = player.getCurrentServer().orElse(null);

        if (!isMuted(playerUUID, playerIp, serverName)) return;

        boolean blockAllCommands = ConfigManager.getBoolean("punishments-system.built-in.mutes.block-all-commands");
        String baseCommand = command.toLowerCase().trim().split(" ")[0];

        boolean shouldBlock = blockAllCommands;

        if (!blockAllCommands) {
            List<String> blockedCommands = ConfigManager.getList("punishments-system.built-in.mutes.blocked-commands");
            shouldBlock = blockedCommands.stream().anyMatch(cmd -> cmd.equalsIgnoreCase(baseCommand));
        }

        if (shouldBlock) {
            Component formattedMuteMessage = formatMuteMessage(playerUUID, serverName);
            player.sendMessage(formattedMuteMessage);
        }
    }

    public void handleMuteQueryMessage(UUID playerUuid, IPlayer sourcePlayer) throws IOException {
        String serverName = sourcePlayer.getCurrentServer().orElse(null);
        boolean isMuted = isMuted(playerUuid, sourcePlayer.getInetAddress(), serverName);

        PunishmentsManager.PunishmentInfo info =
                punishmentsManager.getPunishmentInfo(
                        PunishmentsManager.PunishmentType.MUTE, playerUuid, serverName
                ).join();

        String reason = isMuted && info != null ? info.reason : "";
        long until = isMuted && info != null ? info.getExpiresAt() : 0L;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(playerUuid.toString());
        out.writeBoolean(isMuted);
        out.writeUTF(reason != null ? reason : "");
        out.writeLong(until);
        out.writeUTF(serverName != null ? serverName : "Global");

        byte[] responseData = baos.toByteArray();
        platformAdapter.sendPluginMessage(sourcePlayer, Channel.MUTED_RESPONSE, responseData);
    }

    private boolean isMuted(UUID playerUUID, InetAddress playerIp, String serverName) {
        return punishmentsManager.isPlayerPunished(
                PunishmentsManager.PunishmentType.MUTE, playerUUID, null
        ).join()
                || (serverName != null && punishmentsManager.isPlayerPunished(
                PunishmentsManager.PunishmentType.MUTE, playerUUID, serverName
        ).join())
                || punishmentsManager.isIpPunished(
                PunishmentsManager.PunishmentType.MUTE, playerIp, null
        ).join()
                || (serverName != null && punishmentsManager.isIpPunished(
                PunishmentsManager.PunishmentType.MUTE, playerIp, serverName
        ).join());
    }

    private Component formatBanMessage(String rawMessage, UUID playerUUID, String serverName) {
        PunishmentsManager.PunishmentInfo info = punishmentsManager.getPunishmentInfo(
                PunishmentsManager.PunishmentType.BAN, playerUUID, serverName
        ).join();

        String reason = info != null ? info.reason : null;
        String moderator = "Console";
        String executionDate = info != null ? DateCalculator.formatTimestamp(info.createdAt) : null;
        String duration = info != null ? DateCalculator.formatDuration(info.duration) : null;
        String endDate = info != null && !info.isPermanent()
                ? DateCalculator.formatTimestamp(info.getExpiresAt()) : "Never";
        String timeLeft = info != null ? DateCalculator.formatExpiration(info.createdAt, info.duration) : null;

        String formattedMessage = rawMessage
                .replace("%reason%", reason != null ? reason : "No reason specified")
                .replace("%moderator%", moderator)
                .replace("%date%", executionDate != null ? executionDate : "Unknown")
                .replace("%duration%", duration != null ? duration : "Permanent")
                .replace("%expiration-date%", endDate != null ? endDate : "Unknown")
                .replace("%time-left%", timeLeft != null ? timeLeft : "Permanent");

        String cleanedMessage = formattedMessage.replaceFirst("(?s)\\n\\s*\\z", "");
        return miniMessage.deserialize(cleanedMessage);
    }

    private Component formatLockMessage(String rawMessage, String serverName) {
        var serverLocksManager = bootstrap.getDatabaseUtils().getServerLocksManager();

        String reason = serverLocksManager.getReason(serverName).join();
        String moderator = serverLocksManager.getIssuer(serverName).join();
        long time = serverLocksManager.getTime(serverName).join();

        String formattedMessage = rawMessage
                .replace("%server%", serverName)
                .replace("%reason%", reason != null ? reason : "Unknown")
                .replace("%moderator%", moderator != null ? moderator : "Unknown")
                .replace("%date%", DateCalculator.formatTimestamp(time));

        String cleanedMessage = formattedMessage.replaceFirst("(?s)\\n\\s*\\z", "");
        return miniMessage.deserialize(cleanedMessage);
    }

    private Component formatMuteMessage(UUID playerUUID, String serverName) {
        PunishmentsManager.PunishmentInfo info = punishmentsManager.getPunishmentInfo(
                PunishmentsManager.PunishmentType.MUTE, playerUUID, serverName
        ).join();

        String reason = info != null ? info.reason : null;
        String issuer = "Console";
        long time = info != null ? info.createdAt : 0L;
        long duration = info != null ? info.duration : 0L;

        String rawMuteMessage = LanguageManager.getMessageString("punishments.mute.chat-message");
        if (rawMuteMessage == null || rawMuteMessage.isEmpty()) {
            rawMuteMessage = "§cYou are muted and cannot chat.";
        } else {
            rawMuteMessage = rawMuteMessage
                    .replace("%reason%", reason != null ? reason : "No reason specified")
                    .replace("%moderator%", issuer)
                    .replace("%date%", DateCalculator.formatTimestamp(time))
                    .replace("%duration%", DateCalculator.formatDuration(duration))
                    .replace("%expiration-date%", duration <= 0 ? "Never" : DateCalculator.formatTimestamp(time + duration))
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
            out.writeBoolean(isMuted);
            out.writeUTF(serverName);
            platformAdapter.sendPluginMessage(player, Channel.MUTED, byteStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}