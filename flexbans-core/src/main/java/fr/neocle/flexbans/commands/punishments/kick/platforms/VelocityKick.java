package fr.neocle.flexbans.commands.punishments.kick.platforms;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.commands.punishments.kick.KickPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;
import java.util.UUID;

public class VelocityKick implements KickPlatformHandler {
    private final ProxyServer proxyServer;

    public VelocityKick(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
        Optional<Player> optionalPlayer = proxyServer.getPlayer(uuid);
        return optionalPlayer.isPresent();
    }

    @Override
    public void applyKick(String username, UUID uuid, String sender, String reason) {
        Optional<Player> optionalPlayer = proxyServer.getPlayer(uuid);

        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();

            String rawMessage = LanguageManager.getMessageString("punishments.kick.disconnect-message")
                    .replace("%moderator%", sender)
                    .replace("%reason%", reason);

            if (rawMessage == null || rawMessage.isEmpty()) {
                player.disconnect(Component.text("punishments.ban.disconnect-message"));
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);

            player.disconnect(formattedMessage);
        }
    }
}