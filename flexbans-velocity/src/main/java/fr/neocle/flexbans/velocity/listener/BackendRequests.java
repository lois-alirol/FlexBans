package fr.neocle.flexbans.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.messaging.Channel;
import fr.neocle.flexbans.logger.FlexLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class BackendRequests {
    private final ProxyServer server;
    private static final MinecraftChannelIdentifier BAN_CHANNEL = MinecraftChannelIdentifier.from(Channel.BAN);
    private static final MinecraftChannelIdentifier MUTE_CHANNEL = MinecraftChannelIdentifier.from(Channel.MUTE);
    private static final MinecraftChannelIdentifier KICK_CHANNEL = MinecraftChannelIdentifier.from(Channel.KICK);
    private static final MinecraftChannelIdentifier WARNING_CHANNEL = MinecraftChannelIdentifier.from(Channel.WARNING);

    public BackendRequests(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        MinecraftChannelIdentifier identifier = (MinecraftChannelIdentifier) event.getIdentifier();

        if (identifier.equals(BAN_CHANNEL)) {
            handleIncomingPunishment(event.getData(), "BAN");
        } else if (identifier.equals(MUTE_CHANNEL)) {
            handleIncomingPunishment(event.getData(), "MUTE");
        } else if (identifier.equals(KICK_CHANNEL)) {
            handleIncomingPunishment(event.getData(), "KICK");
        } else if (identifier.equals(WARNING_CHANNEL)) {
            handleIncomingPunishment(event.getData(), "WARNING");
        }
    }

    private void handleIncomingPunishment(byte[] data, String actionType) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String targetName = in.readUTF();
            String sender = in.readUTF();
            String duration = in.readUTF();
            String reason = in.readUTF();
            String scope = in.readUTF();
            boolean silent = in.readBoolean();
            boolean ipScope = in.readBoolean();

            executeAction(actionType, targetName, sender, duration, reason, scope, silent, ipScope);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeAction(String type, String target, String sender, String duration,
                               String reason, String scope, boolean silent, boolean ip) {
        String serverOrigin = server.getPlayer(sender)
                .flatMap(Player::getCurrentServer)
                .map(connection -> connection.getServerInfo().getName())
                .orElse("unknown");

        switch (type) {
            case "BAN" -> {
                FlexBansAPI.getInstance()
                           .getBanExecutor()
                           .executeBan(target, sender, duration, reason,
                                       scope, serverOrigin, silent, ip,
                                       message -> server.getPlayer(sender)
                                           .ifPresent(
                                                   player -> player.sendMessage(Component.text(message))
                                           )

                           );
            }
            case "MUTE" -> {
                FlexBansAPI.getInstance()
                        .getMuteExecutor()
                        .executeMute(target, sender, duration, reason,
                                scope, serverOrigin, silent, ip,
                                message -> server.getPlayer(sender)
                                                        .ifPresent(
                                                                player -> player.sendMessage(Component.text(message))
                                                        )

                        );
            }
            case "KICK" -> {
                FlexBansAPI.getInstance()
                        .getKickExecutor()
                        .executeKick(
                                target, sender, reason,
                                serverOrigin, silent, ip,
                                message -> server.getPlayer(sender)
                                        .ifPresent(player ->
                                                player.sendMessage(Component.text(message))
                                        )
                        );
            }
            case "WARNING" -> {
                FlexBansAPI.getInstance()
                        .getWarningExecutor()
                        .executeWarning(
                                target, sender, reason,
                                scope, serverOrigin, silent, ip,
                                message -> server.getPlayer(sender)
                                        .ifPresent(
                                                player -> player.sendMessage(Component.text(message))
                                        )
                        );
            }
        }
    }
}
