package fr.neocle.flexbans.velocity. listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection. PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.common.listener.PlayerEventHandler;
import fr.neocle.flexbans.velocity.command.adapter.VelocityPlatform;
import fr.neocle.flexbans.velocity.command.adapter.VelocityPlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class PlayerEvents {
    private final PlayerEventHandler commonHandler;
    private final VelocityPlatform adapter;
    public static final MinecraftChannelIdentifier MUTE_QUERY_CHANNEL = MinecraftChannelIdentifier.from("muting:query");

    public PlayerEvents(PlayerEventHandler commonHandler, VelocityPlatform adapter) {
        this.commonHandler = commonHandler;
        this.adapter = adapter;
    }

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        commonHandler.handlePlayerLogin(new VelocityPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onPlayerServerConnect(ServerPreConnectEvent event) {
        String targetServer = event.getOriginalServer().getServerInfo().getName();
        commonHandler.handlePlayerServerConnect(
                new VelocityPlayer(event.getPlayer()),
                targetServer
        );
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        commonHandler.handlePlayerChat(new VelocityPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof com.velocitypowered.api.proxy.Player player)) return;
        commonHandler.handleCommandExecute(new VelocityPlayer(player), event.getCommand());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (! event.getIdentifier().equals(MUTE_QUERY_CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection server)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            UUID uuid = UUID.fromString(in.readUTF());
            var player = adapter.getPlayer(uuid);
            if (player. isPresent()) {
                commonHandler.handleMuteQueryMessage(uuid, player.get());
            }
        } catch (IOException e) {
            e. printStackTrace();
        }
    }
}