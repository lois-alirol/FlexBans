package fr.neocle.flexbans.commands.punishments.kick.platforms;

import fr.neocle.flexbans.commands.punishments.kick.KickPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeeKick implements KickPlatformHandler {
    private final ProxyServer proxyServer;

    public BungeeKick(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        ProxiedPlayer player = proxyServer.getPlayer(uuid);
        return player != null;
    }

    @Override
    public void applyKick(String username, UUID uuid, String sender, String reason) {
        ProxiedPlayer player = proxyServer.getPlayer(uuid);

        if (player != null) {
            String rawMessage = LanguageManager.getMessageString("punishments.kick.disconnect-message")
                    .replace("%moderator%", sender)
                    .replace("%reason%", reason);

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.disconnect(LanguageManager.getBungeeMessageComponent(player,"punishments.kick.disconnect-message"));
                return;
            }

            LanguageManager.languageData.put("punishments.kick.disconnect-message.temp", rawMessage);
            BaseComponent[] components = LanguageManager.getBungeeMessageComponent(player, "punishments.kick.disconnect-message.temp");
            player.disconnect(components);

            LanguageManager.languageData.remove("punishments.kick.disconnect-message.temp");
        }
    }
}
