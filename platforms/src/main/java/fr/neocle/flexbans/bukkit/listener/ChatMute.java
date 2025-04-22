package fr.neocle.flexbans.bukkit.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatMute implements Listener, PluginMessageListener {
    private final Map<UUID, MuteInfo> mutedPlayers = new HashMap<>();
    private final JavaPlugin plugin;

    private static final String MUTE_CHANNEL = "muting:channel";
    private static final String MUTE_RESPONSE = "muting:response";
    private static final String MUTE_QUERY = "muting:query";

    public ChatMute(JavaPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "muting:channel", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "muting:response", this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "muting:query");
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            if (channel.equals(MUTE_RESPONSE)) {
                if (in.available() < 4) {
                    plugin.getLogger().warning("Received malformed muting:response message (too short)");
                    return;
                }

                UUID uuid = UUID.fromString(in.readUTF());

                if (in.available() < 1) {
                    plugin.getLogger().warning("Received incomplete muting:response message for player " + uuid);
                    return;
                }

                boolean isMuted = in.readBoolean();

                if (in.available() > 0) {
                    String reason = in.readUTF();

                    if (in.available() > 0) {
                        long until = in.readLong();

                        if (in.available() > 0) {
                            String muteServer = in.readUTF();

                            if (isMuted) {
                                mutedPlayers.put(uuid, new MuteInfo(reason, until, muteServer));
                            } else {
                                mutedPlayers.remove(uuid);
                            }
                        }
                    }
                }
            } else if (channel.equals(MUTE_CHANNEL)) {
                if (in.available() < 4) {
                    plugin.getLogger().warning("Received malformed muting:channel message (too short)");
                    return;
                }

                UUID uuid = UUID.fromString(in.readUTF());

                if (in.available() < 1) {
                    plugin.getLogger().warning("Received incomplete muting:channel message for player " + uuid);
                    return;
                }

                boolean isMuted = in.readBoolean();

                String muteServer = "Global";
                if (in.available() > 0) {
                    muteServer = in.readUTF();
                }

                if (isMuted) {
                    mutedPlayers.put(uuid, new MuteInfo("", 0, muteServer));
                    requestMuteStatus(plugin.getServer().getPlayer(uuid));
                } else {
                    mutedPlayers.remove(uuid);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Error processing plugin message on channel " + channel +
                    ": " + e.getMessage() +
                    " (Message length: " + message.length + " bytes)");
            if (!(e instanceof EOFException)) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        requestMuteStatus(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        requestMuteStatus(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        MuteInfo muteInfo = mutedPlayers.get(uuid);
        if (muteInfo == null) {
            requestMuteStatus(player);
            return;
        }

        if (muteInfo.until > 0 && System.currentTimeMillis() > muteInfo.until) {
            mutedPlayers.remove(uuid);
            requestMuteStatus(player);
            return;
        }

        event.setCancelled(true);
    }

    private void requestMuteStatus(Player player) {
        if (player == null) return;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DataOutputStream data = new DataOutputStream(out)) {
            data.writeUTF(player.getUniqueId().toString());

            player.sendPluginMessage(plugin, MUTE_QUERY, out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private record MuteInfo(String reason, long until, String server) {
    }
}