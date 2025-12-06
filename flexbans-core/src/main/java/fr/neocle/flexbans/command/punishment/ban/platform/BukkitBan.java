package fr.neocle.flexbans.command.punishment.ban.platform;

import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDate;
import java.util.UUID;

public class BukkitBan implements BanPlatformHandler {
    @Override
    public void applyBan(String target, UUID uuid, String issuer_name, String duration,
                         String reason, String serverScope, boolean isIpBan) {
        Player player = uuid != null
                ? Bukkit.getPlayer(uuid)
                : Bukkit.getPlayer(target);

        if (player != null) {
            String rawMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuer_name)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);

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