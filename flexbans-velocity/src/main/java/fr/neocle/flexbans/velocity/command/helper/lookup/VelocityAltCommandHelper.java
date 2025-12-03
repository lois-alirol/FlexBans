package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.alt.IAltCommandHelper;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.punishment.HistoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VelocityAltCommandHelper implements IAltCommandHelper {
    private final ProxyServer proxyServer;
    private final HistoryManager historyManager;

    public VelocityAltCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.proxyServer = proxyServer;
        this.historyManager = databaseUtils.getHistoryManager();
    }

    @Override
    public String getPlayerIP(String playerName) {
        java.util.Optional<Player> optPlayer = proxyServer.getPlayer(playerName);
        if (optPlayer.isPresent()) {
            return optPlayer.get(). getRemoteAddress().getAddress().getHostAddress();
        }

        return historyManager.getPlayerIP(playerName);
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