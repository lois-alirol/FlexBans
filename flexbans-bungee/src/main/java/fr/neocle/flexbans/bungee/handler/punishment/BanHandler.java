package fr.neocle.flexbans.bungee.handler.punishment;

import fr.neocle.flexbans.api.platform.handler.BanPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.LocalDate;
import java.util.UUID;

public class BanHandler implements BanPlatformHandler {
    private final ProxyServer proxy;

    public BanHandler(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void applyBan(String target, UUID uuid, String issuer_name, String duration,
                         String reason, String serverScope, boolean isIpBan) {
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