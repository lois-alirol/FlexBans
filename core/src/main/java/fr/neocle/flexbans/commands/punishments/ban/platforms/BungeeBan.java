package fr.neocle.flexbans.commands.punishments.ban.platforms;

import fr.neocle.flexbans.commands.punishments.ban.BanPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.LocalDate;
import java.util.UUID;

public class BungeeBan implements BanPlatformHandler {
    private final ProxyServer proxy;

    public BungeeBan(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void applyBan(String target, UUID uuid, String issuer_name, String duration,
                         String reason, String serverScope) {
        ProxiedPlayer player = uuid != null
                ? proxy.getPlayer(uuid)
                : proxy.getPlayer(target);

        if (player != null) {
            String rawMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message");

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.disconnect(LanguageManager.getBungeeMessageComponent(player,"punishments.ban.disconnect-message"));
                return;
            }

            rawMessage = rawMessage
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuer_name)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);

            LanguageManager.languageData.put("punishments.ban.disconnect-message.temp", rawMessage);
            BaseComponent[] components = LanguageManager.getBungeeMessageComponent(player, "punishments.ban.disconnect-message.temp");
            player.disconnect(components);

            LanguageManager.languageData.remove("punishments.ban.disconnect-message.temp");
        }
    }
}