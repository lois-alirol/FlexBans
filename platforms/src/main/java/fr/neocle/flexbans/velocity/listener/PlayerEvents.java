package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.database.Punishments.BansManager;
import fr.neocle.flexbans.database.Punishments.HistoryManager;
import fr.neocle.flexbans.database.Punishments.MutesManager;
import fr.neocle.flexbans.database.Servers.ServerLocksManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.DateCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;

public class PlayerEvents {
    private final Bootstrap bootstrap;
    private final ProxyServer proxyServer;
    private final BansManager bansManager;
    private final MutesManager mutesManager;
    private final ServerLocksManager serverLocksManager;

    public static final MinecraftChannelIdentifier MUTE_QUERY_CHANNEL = MinecraftChannelIdentifier.from("muting:query");
    public static final MinecraftChannelIdentifier MUTE_RESPONSE_CHANNEL = MinecraftChannelIdentifier.from("muting:response");

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("muting:channel");

    public PlayerEvents(Bootstrap bootstrap, ProxyServer proxyServer) {
        this.bootstrap = bootstrap;
        this.proxyServer = proxyServer;
        this.bansManager = bootstrap.getDatabaseUtils().getBansManager();
        this.mutesManager = bootstrap.getDatabaseUtils().getMutesManager();
        this.serverLocksManager = bootstrap.getDatabaseUtils().getServerLocksManager();

        proxyServer.getChannelRegistrar().register(IDENTIFIER);
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        String playerIp = ((InetSocketAddress) player.getRemoteAddress()).getAddress().getHostAddress();

        HistoryManager historyManager = bootstrap.getDatabaseUtils().getHistoryManager();

        if (!historyManager.playerExists(playerUuid)) {
            historyManager.insertPlayerData(playerUuid, player.getUsername(), playerIp);
        }
    }

    @Subscribe
    public void onPlayerServerConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        UUID targetUUID = player.getUniqueId();
        String playerIp = ((InetSocketAddress) player.getRemoteAddress()).getAddress().getHostAddress();

        RegisteredServer destinationServer = event.getOriginalServer();
        String serverName = destinationServer.getServerInfo().getName();

        String rawBanMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message");
        String rawLockMessage = LanguageManager.getMessageString("server-locks.disconnect-message");
        MiniMessage miniMessage = MiniMessage.miniMessage();

        if (bansManager.isPlayerBanned(targetUUID, null)) {
            String reason = bansManager.getReason(targetUUID, null);
            String moderator = bansManager.getIssuer(targetUUID, null);
            String executionDate = DateCalculator.formatTimestamp(bansManager.getTime(targetUUID, null));
            String duration = DateCalculator.formatDuration(bansManager.getDuration(targetUUID, null));
            String endDate = bansManager.getDuration(targetUUID, serverName) <= 0 ? "Never" : DateCalculator.formatTimestamp(bansManager.getTime(targetUUID, serverName) + bansManager.getDuration(targetUUID, serverName));
            String timeLeft = DateCalculator.formatExpiration(bansManager.getTime(targetUUID, null),
                    bansManager.getDuration(targetUUID, null));

            rawBanMessage = rawBanMessage
                    .replace("%reason%", reason != null ? reason : "No reason specified")
                    .replace("%moderator%", moderator != null ? moderator : "Console")
                    .replace("%date%", executionDate != null ? executionDate : "Unknown")
                    .replace("%duration%", duration != null ? duration : "Permanent")
                    .replace("%expiration-date%", endDate != null ? endDate : "Unknown")
                    .replace("%time-left%", timeLeft != null ? timeLeft : "Permanent");

            Component formattedBanMessage = miniMessage.deserialize(rawBanMessage);

            player.disconnect(formattedBanMessage);
            return;
        }

        if (bansManager.isPlayerBanned(targetUUID, serverName) ||
                bansManager.isIpBanned(playerIp, serverName)) {
            String reason = bansManager.getReason(targetUUID, serverName);
            String moderator = bansManager.getIssuer(targetUUID, serverName);
            String executionDate = DateCalculator.formatTimestamp(bansManager.getTime(targetUUID, serverName));
            String duration = DateCalculator.formatDuration(bansManager.getDuration(targetUUID, serverName));
            String endDate = bansManager.getDuration(targetUUID, serverName) <= 0 ? "Never" : DateCalculator.formatTimestamp(bansManager.getTime(targetUUID, serverName) + bansManager.getDuration(targetUUID, serverName));
            String timeLeft = DateCalculator.formatExpiration(bansManager.getTime(targetUUID, serverName),
                    bansManager.getDuration(targetUUID, serverName));

            rawBanMessage = rawBanMessage
                    .replace("%reason%", reason != null ? reason : "No reason specified")
                    .replace("%moderator%", moderator != null ? moderator : "Console")
                    .replace("%date%", executionDate != null ? executionDate : "Unknown")
                    .replace("%duration%", duration != null ? duration : "Permanent")
                    .replace("%expiration-date%", endDate != null ? endDate : "Unknown")
                    .replace("%time-left%", timeLeft != null ? timeLeft : "Permanent");

            Component formattedBanMessage = miniMessage.deserialize(rawBanMessage);

            if (player.getCurrentServer().isEmpty()) {
                player.disconnect(formattedBanMessage);
                return;
            }

            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(formattedBanMessage);
            return;
        }

        if (player.hasPermission("flexbans.serverlock.bypass")) {
            return;
        }

        if (serverLocksManager.isServerLocked("Global")) {
            rawLockMessage = rawLockMessage
                    .replace("%server%", "Global")
                    .replace("%reason%", serverLocksManager.getReason("Global"))
                    .replace("%moderator%", serverLocksManager.getIssuer("Global"))
                    .replace("%date%", DateCalculator.formatTimestamp(serverLocksManager.getTime("Global")));

            Component formattedLockMessage = miniMessage.deserialize(rawLockMessage);

            player.disconnect(formattedLockMessage);
        }

        if (serverLocksManager.isServerLocked(serverName)) {
            rawLockMessage = rawLockMessage
                    .replace("%server%", serverName)
                    .replace("%reason%", serverLocksManager.getReason(serverName))
                    .replace("%moderator%", serverLocksManager.getIssuer(serverName))
                    .replace("%date%", DateCalculator.formatTimestamp(serverLocksManager.getTime(serverName)));

            Component formattedLockMessage = miniMessage.deserialize(rawLockMessage);

            if (player.getCurrentServer().isEmpty()) {
                player.disconnect(formattedLockMessage);
                return;
            }

            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(formattedLockMessage);
            return;
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String serverName = player.getCurrentServer()
                .map(server -> server.getServer().getServerInfo().getName())
                .orElse(null);
        String playerIp = ((InetSocketAddress) player.getRemoteAddress()).getAddress().getHostAddress();

        if (mutesManager.isPlayerMuted(playerUUID, null) ||
                (serverName != null && mutesManager.isPlayerMuted(playerUUID, serverName)) ||
                mutesManager.isIpMuted(playerIp, null) ||
                (serverName != null && mutesManager.isIpMuted(playerIp, serverName))) {

            String reason = mutesManager.getReason(playerUUID, serverName);
            String issuer = mutesManager.getIssuer(playerUUID, serverName);
            long time = mutesManager.getTime(playerUUID, serverName);
            long duration = mutesManager.getDuration(playerUUID, serverName);

            String rawMuteMessage = LanguageManager.getMessageString("punishments.mute.chat-message");
            if (rawMuteMessage == null || rawMuteMessage.isEmpty()) {
                rawMuteMessage = "§cYou are muted and cannot chat.";
            } else {
                rawMuteMessage = rawMuteMessage
                        .replace("%reason%", reason != null ? reason : "No reason specified")
                        .replace("%moderator%", issuer != null ? issuer : "Console")
                        .replace("%date%", DateCalculator.formatTimestamp(time))
                        .replace("%duration%", DateCalculator.formatDuration(duration))
                        .replace("%expiration-date%", duration <= 0 ? "Never" : DateCalculator.formatTimestamp(time + duration))
                        .replace("%time-left%", DateCalculator.formatExpiration(time, duration));
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMuteMessage = miniMessage.deserialize(rawMuteMessage);

            player.sendMessage(formattedMuteMessage);
            sendMuteSignal(player, true, serverName);
        }
    }

    private void sendMuteSignal(Player player, boolean isMuted, String serverName) {
        player.getCurrentServer().ifPresent(connection -> {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteStream);

            try {
                out.writeUTF(player.getUniqueId().toString());
                out.writeBoolean(isMuted);
                out.writeUTF(serverName);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            connection.sendPluginMessage(IDENTIFIER, byteStream.toByteArray());
        });
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(MUTE_QUERY_CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection server)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            UUID uuid = UUID.fromString(in.readUTF());

            Player player = proxyServer.getPlayer(uuid).orElse(null);
            if (player == null) return;

            String serverName = player.getCurrentServer()
                    .map(connection -> connection.getServer().getServerInfo().getName())
                    .orElse(null);

            boolean isMuted = mutesManager.isPlayerMuted(uuid, null) ||
                    (serverName != null && mutesManager.isPlayerMuted(uuid, serverName));

            String reason = isMuted ? mutesManager.getReason(uuid, serverName) : "";
            long until = isMuted ? mutesManager.getExpiration(uuid, serverName) : 0L;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);

            out.writeUTF(uuid.toString());
            out.writeBoolean(isMuted);
            out.writeUTF(reason != null ? reason : "");
            out.writeLong(until);
            out.writeUTF(serverName != null ? serverName : "Global");

            server.sendPluginMessage(MUTE_RESPONSE_CHANNEL, baos.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
