package fr.neocle.flexbans.commands.punishments.kick.platforms;

import fr.neocle.flexbans.commands.punishments.kick.KickPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class BukkitKick implements KickPlatformHandler {

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline();
    }

    @Override
    public void applyKick(String username, UUID uuid, String sender, String reason) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            String rawMessage = LanguageManager.getMessageString("punishments.kick.disconnect-message")
                    .replace("%moderator%", sender)
                    .replace("%reason%", reason);

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.kick(LanguageManager.getMessageComponent("punishments.ban.disconnect-message"));
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);

            player.kick(formattedMessage);
        }
    }
}