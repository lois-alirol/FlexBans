package fr.neocle.flexbans.util.broadcast;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BroadcasterBungee implements Broadcaster {
    private final ProxyServer proxyServer;

    public BroadcasterBungee(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(String message) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (ProxiedPlayer player : proxyServer.getPlayers()) {
            player.sendMessage(formattedMessage);
        }
    }

    @Override
    public void execute(String message, String permission) {

    }
}
