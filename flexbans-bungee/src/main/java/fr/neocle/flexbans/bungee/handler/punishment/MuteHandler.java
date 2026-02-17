package fr.neocle.flexbans.bungee.handler.punishment;

import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.LocalDate;
import java.util.UUID;

public class MuteHandler implements MutePlatformHandler {
    private final ProxyServer proxy;

    public MuteHandler(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void applyMute(String target, UUID uuid, String issuerName, String duration,
                          String reason, String serverScope) {
        ProxiedPlayer player = uuid != null
                ? proxy.getPlayer(uuid)
                : proxy.getPlayer(target);

        if (player != null) {
            if (serverScope != null && !serverScope.isEmpty() && !serverScope.equalsIgnoreCase("global")) {
                if (player.getServer() == null ||
                        !player.getServer().getInfo().getName().equalsIgnoreCase(serverScope)) {
                    return;
                }
            }

            String rawMessage = LanguageManager.getMessageString("punishments.mute.alert-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuerName)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(player,"punishments.mute.alert-message"));
                return;
            }

            LanguageManager.languageData.put("punishments.mute.alert-message", rawMessage);
            BaseComponent[] components = LanguageManager.getBungeeMessageComponent(player, "punishments.mute.alert-message.temp");
            player.sendMessage(components);

            LanguageManager.languageData.remove("punishments.mute.alert-message.temp");
        }
    }
}