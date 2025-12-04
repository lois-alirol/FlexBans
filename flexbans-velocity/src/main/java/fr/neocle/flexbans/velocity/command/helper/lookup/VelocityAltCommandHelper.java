package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.alt.IAltCommandHelper;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.player.ProfilesManager;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VelocityAltCommandHelper implements IAltCommandHelper {
    private final ProxyServer proxyServer;

    public VelocityAltCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.proxyServer = proxyServer;
    }

    @Override
    public InetAddress getPlayerInetAddress(String playerName) {
        java.util.Optional<Player> optPlayer = proxyServer.getPlayer(playerName);
        if (optPlayer.isPresent()) {
            return optPlayer.get(). getRemoteAddress().getAddress();
        }

        return null;
    }

    @Override
    public boolean isPlayerOnline(UUID playerUuid) {
        return proxyServer.getPlayer(playerUuid).isPresent();
    }

    @Override
    public List<String> getOnlinePlayerSuggestions(String partialName) {
        List<String> suggestions = new ArrayList<>();

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(partialName)) {
                suggestions.add(player.getUsername());
            }
        }

        return suggestions;
    }
}