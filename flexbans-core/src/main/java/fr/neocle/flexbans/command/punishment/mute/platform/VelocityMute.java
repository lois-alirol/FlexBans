package fr.neocle.flexbans.command.punishment.mute.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.mute.MutePlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDate;
import java.util.UUID;

public class VelocityMute implements MutePlatformHandler {
    private final ProxyServer proxy;

    public VelocityMute(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void applyMute(String target, UUID uuid, String issuer_name, String duration,
                         String reason, String serverScope) {
        Player player = uuid != null
                ? proxy.getPlayer(uuid).orElse(null)
                : proxy.getPlayer(target).orElse(null);

        if (player != null) {

            if (serverScope != null && !serverScope.isEmpty() && !serverScope.equalsIgnoreCase("global")) {
                if (!player.getCurrentServer().isPresent() ||
                        !player.getCurrentServer().get().getServerInfo().getName().equalsIgnoreCase(serverScope)) {
                    return;
                }
            }

            String rawMessage = LanguageManager.getMessageString("punishments.mute.alert-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuer_name)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);


            if (rawMessage == null || rawMessage.isEmpty()) {
                player.disconnect(Component.text("punishments.mute.alert-message"));
                return;
            }

            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component formattedMessage = miniMessage.deserialize(rawMessage);

            player.sendMessage(formattedMessage);
        }
    }
}
