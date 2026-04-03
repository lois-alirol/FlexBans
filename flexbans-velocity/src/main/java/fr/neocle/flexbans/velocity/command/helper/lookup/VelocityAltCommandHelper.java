package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.alt.IAltCommandHelper;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityAltCommandHelper implements IAltCommandHelper {
    private final ProxyServer proxyServer;
    private final ProfilesManager profilesManager;

    private static final FlexLogger LOGGER = FlexLogger.get(VelocityAltCommandHelper.class);

    public VelocityAltCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.proxyServer = proxyServer;
        this.profilesManager = databaseUtils.getProfilesManager();
    }

    @Override
    public InetAddress getPlayerInetAddress(String playerName) {
        Optional<Player> optPlayer = proxyServer.getPlayer(playerName);

        if (optPlayer.isPresent()) {
            InetAddress address = optPlayer.get().getRemoteAddress().getAddress();
            LOGGER.debug("Player '{}' is online, IP resolved from Proxy: {}", playerName, address);
            return address;
        }

        LOGGER.debug("Player '{}' is offline, looking up IP in database...", playerName);
        try {
            return profilesManager.getIp(playerName).join();
        } catch (Exception e) {
            LOGGER.error("Failed to resolve IP from database for '{}'", playerName, e);
            return null;
        }
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