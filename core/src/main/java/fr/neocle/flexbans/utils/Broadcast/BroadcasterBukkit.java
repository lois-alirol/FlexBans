package fr.neocle.flexbans.utils.Broadcast;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BroadcasterBukkit implements Broadcaster {
    @Override
    public void execute(String message) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
    }
}
