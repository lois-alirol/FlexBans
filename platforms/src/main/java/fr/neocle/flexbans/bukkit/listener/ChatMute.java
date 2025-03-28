package fr.neocle.flexbans.bukkit.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatMute implements Listener, PluginMessageListener {
    private final Set<UUID> mutedPlayers = new HashSet<>();

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("muting:channel")) return;

        System.out.println("Plugin message received!");

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            UUID uuid = UUID.fromString(in.readUTF());

            System.out.println("Parsed UUID: " + uuid);
            mutedPlayers.add(uuid);

            System.out.println("Muted players after addition: " + mutedPlayers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        System.out.println("playerchat " + event.getPlayer().getUniqueId() + mutedPlayers);
        if (mutedPlayers.contains(event.getPlayer().getUniqueId())) {

            System.out.println("muted player");

            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("§cYou are muted and nobody can see your messages."));
        }
    }
}
