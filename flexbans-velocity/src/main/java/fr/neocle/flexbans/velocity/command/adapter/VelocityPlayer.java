package fr.neocle.flexbans.velocity.command.adapter;

import com.velocitypowered.api.proxy.Player;
import fr.neocle.flexbans.common.adapter.IPlayer;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java. util.Optional;
import java.util.UUID;

public class VelocityPlayer implements IPlayer {
    private final Player player;

    public VelocityPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getUsername() {
        return player. getUsername();
    }

    @Override
    public String getIp() {
        return ((InetSocketAddress) player.getRemoteAddress()).getAddress().getHostAddress();
    }

    @Override
    public Optional<String> getCurrentServer() {
        return player.getCurrentServer().map(connection -> connection.getServer().getServerInfo().getName());
    }

    @Override
    public void disconnect(Object formattedMessage) {
        player.disconnect((Component) formattedMessage);
    }

    @Override
    public void sendMessage(Object formattedMessage) {
        player. sendMessage((Component) formattedMessage);
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}