package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.database.Punishments.HistoryManager;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class PlayerEvents {
    private final Bootstrap bootstrap;
    private final ProxyServer proxyServer;

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("muting:channel");

    public PlayerEvents(Bootstrap bootstrap, ProxyServer proxyServer) {
        this.bootstrap = bootstrap;
        this.proxyServer = proxyServer;

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

        if (bootstrap.getDatabaseUtils().getBansManager().isPlayerBanned(targetUUID) ||
                bootstrap.getDatabaseUtils().getBansManager().isIpBanned(playerIp)) {
            String rawMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message");

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.disconnect(Component.text("punishments.ban.disconnect-message"));
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);

            player.disconnect(formattedMessage);
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        if ("c86eca4d-cb44-4589-98f7-3f6554cf0823".equals(player.getUniqueId().toString())) {
            player.sendMessage(Component.text("§cYou are muted and cannot chat."));

            sendMuteSignal(player);
        }
    }

    private void sendMuteSignal(Player player) {
        player.getCurrentServer().ifPresent(connection -> {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteStream);

            try {
                out.writeUTF(player.getUniqueId().toString());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            connection.sendPluginMessage(IDENTIFIER, byteStream.toByteArray());
        });
    }
}
