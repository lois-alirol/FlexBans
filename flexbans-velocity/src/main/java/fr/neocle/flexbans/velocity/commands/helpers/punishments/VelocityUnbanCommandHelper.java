package fr.neocle.flexbans.velocity.commands.helpers.punishments;

import com.velocitypowered.api.proxy. Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.unban.IUnbanCommandHelper;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityUnbanCommandHelper implements IUnbanCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityUnbanCommandHelper(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public boolean isServerRegistered(String serverName) {
        return proxyServer.getServer(serverName).  isPresent();
    }

    @Override
    public List<String> getRegisteredServers() {
        List<String> servers = proxyServer.getAllServers().stream()
                .  map(server -> server.getServerInfo(). getName())
                .  collect(Collectors.toList());
        servers.add("Global");
        return servers;
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        return proxyServer.getAllPlayers(). stream()
                .map(Player::getUsername)
                .collect(Collectors.toList());
    }
}