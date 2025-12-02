package fr.neocle.flexbans.velocity.commands.helpers.punishments;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.ban.IBanCommandHelper;
import fr.neocle.flexbans.velocity.commands.utils.PluginMessageUtil;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityBanCommandHelper implements IBanCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityBanCommandHelper(ProxyServer proxyServer) {
        this. proxyServer = proxyServer;
    }

    @Override
    public void sendDialogMessage(String playerName, String dialogType) {
        proxyServer.getPlayer(playerName).ifPresent(player ->
                PluginMessageUtil. sendDialogMessage(player, dialogType)
        );
    }

    @Override
    public boolean isServerRegistered(String serverName) {
        return proxyServer.getServer(serverName). isPresent();
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        return proxyServer. getAllPlayers().stream()
                . map(Player::getUsername)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRegisteredServers() {
        return proxyServer.getAllServers().stream()
                . map(server -> server.getServerInfo().getName())
                .collect(Collectors.toList());
    }
}
