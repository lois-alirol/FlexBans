package fr.neocle.flexbans.utils.Broadcast;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class BroadcasterBungee implements Broadcaster {
    private final Plugin plugin;

    public BroadcasterBungee(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(String message) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            player.sendMessage(formattedMessage);
        }
    }
}
