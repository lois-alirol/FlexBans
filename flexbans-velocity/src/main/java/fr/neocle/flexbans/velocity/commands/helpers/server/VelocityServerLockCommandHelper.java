package fr.neocle.flexbans.velocity.commands.helpers.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr.neocle.flexbans.common.commands.server.lock.IServerLockCommandHelper;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandSource;

import java.util.List;
import java.util.stream.Collectors;

public class VelocityServerLockCommandHelper implements IServerLockCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityServerLockCommandHelper(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getDefaultServerName(ICommandSource source) {
        if (source instanceof VelocityCommandSource velocitySource) {
            String origin = velocitySource.getOrigin();
            if (!origin.equals("Proxy")) {
                return origin;
            }
        }
        return "Global";
    }

    @Override
    public boolean isServerRegistered(String serverName) {
        return proxyServer.getServer(serverName). isPresent();
    }

    @Override
    public List<String> getRegisteredServers() {
        List<String> servers = proxyServer.getAllServers().stream()
                . map(server -> server.getServerInfo().getName())
                .collect(Collectors.toList());
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
