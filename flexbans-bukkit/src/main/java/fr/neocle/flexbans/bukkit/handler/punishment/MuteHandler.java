package fr.neocle.flexbans.bukkit.handler.punishment;

import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.UUID;

public class MuteHandler implements MutePlatformHandler {
    @Override
    public void applyMute(String target, UUID uuid, String issuerName, String duration,
                          String reason, String serverScope) {
        Player player = uuid != null
                ? Bukkit.getPlayer(uuid)
                : Bukkit.getPlayer(target);

        if (player != null) {
            String rawMessage = LanguageManager.getMessageString("punishments.mute.alert-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuerName)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.sendMessage("punishments.mute.alert-message");
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);
            
            player.sendMessage(formattedMessage);
        }
    }
}