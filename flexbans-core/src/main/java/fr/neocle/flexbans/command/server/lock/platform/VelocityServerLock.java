package fr.neocle.flexbans.command.server.lock.platform;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.neocle.flexbans.command.server.lock.ServerLockPlatformHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.util.DateCalculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Optional;

public class VelocityServerLock implements ServerLockPlatformHandler {
    private final ProxyServer proxyServer;

    public VelocityServerLock(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void applyLock(String serverName, String reason, String sender, long date, String duration) {
        MiniMessage miniMessage = MiniMessage.miniMessage();

        String rawMessage = LanguageManager.getMessageString("server-locks.disconnect-message")
                .replace("%server%", serverName)
                .replace("%reason%", reason)
                .replace("%moderator%", sender)
                .replace("%date%", DateCalculator.formatTimestamp(date))
                .replace("%duration%", duration);

        Component formattedMessage = miniMessage.deserialize(rawMessage);

        if (serverName.equalsIgnoreCase("Global")) {
            for (Player player : proxyServer.getAllPlayers()) {
                if (!player.hasPermission("flexbans.serverlock.bypass")) {
                    player.disconnect(formattedMessage);
                }
            }
        } else {
            Optional<RegisteredServer> optionalServer = proxyServer.getServer(serverName);
            if (optionalServer.isEmpty()) return;

            RegisteredServer targetServer = optionalServer.get();

            for (Player player : proxyServer.getAllPlayers()) {
                if (player.getCurrentServer().isPresent()
                        && player.getCurrentServer().get().getServerInfo().equals(targetServer.getServerInfo())
                        && !player.hasPermission("flexbans.serverlock.bypass")) {
                    player.disconnect(formattedMessage);
                }
            }
        }
    }
}