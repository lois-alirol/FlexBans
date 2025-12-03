package fr.neocle.flexbans.command.punishment.ban.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.LocalDate;
import java.util.UUID;

public class VelocityBan implements BanPlatformHandler {
    private final ProxyServer proxy;

    public VelocityBan(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void applyBan(String target, UUID uuid, String issuer_name, String duration,
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

            String rawMessage = LanguageManager.getMessageString("punishments.ban.disconnect-message")
                    .replace("%reason%", reason)
                    .replace("%moderator%", issuer_name)
                    .replace("%date%", LocalDate.now().toString())
                    .replace("%duration%", duration);


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
