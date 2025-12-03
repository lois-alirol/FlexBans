package fr.neocle.flexbans.velocity.command.adapter;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import fr.neocle.flexbans.common.adapter.IPlatform;
import fr.neocle.flexbans.common.adapter.IPlayer;

import java.util.Optional;
import java.util.UUID;

public class VelocityPlatform implements IPlatform {
    private final ProxyServer proxyServer;

    public VelocityPlatform(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public Optional<IPlayer> getPlayer(UUID uuid) {
        return proxyServer.getPlayer(uuid).map(VelocityPlayer::new);
    }

    @Override
    public void sendPluginMessage(IPlayer player, String channel, byte[] data) {
        proxyServer.getPlayer(player.getUniqueId()).ifPresent(velocityPlayer -> {
            velocityPlayer.getCurrentServer().ifPresent(connection -> {
                connection.sendPluginMessage(
                        MinecraftChannelIdentifier.from(channel),
                        data
                );
            });
        });
    }
}