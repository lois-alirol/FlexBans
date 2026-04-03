package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.history.IHistoryCommandHelper;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityHistoryCommandHelper implements IHistoryCommandHelper {
    private final ProxyServer proxyServer;
    private final ProfilesManager profilesManager;
    private final DatabaseConnectionManager dbManager;
    private final SimpleDateFormat dateFormat;

    private static final FlexLogger LOGGER = FlexLogger.get(VelocityHistoryCommandHelper.class);

    public VelocityHistoryCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                                        DatabaseConnectionManager dbManager) {
        this.proxyServer = proxyServer;
        this.profilesManager = databaseUtils.getProfilesManager();
        this.dbManager = dbManager;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    }

    @Override
    public UUID getPlayerUuid(String playerName) {
        Optional<Player> optPlayer = proxyServer.getPlayer(playerName);
        if (optPlayer.isPresent()) {
            return optPlayer.get().getUniqueId();
        }

        UUID uuid = null;
        try {
            uuid = profilesManager.getUuid(playerName).join();
        } catch (Exception e) {
            LOGGER.error("Failed to resolve UUID from database for '{}'", playerName, e);
        }

        if (uuid != null) {
            LOGGER.debug("Player '{}' found in database: {}", playerName, uuid);
        }
        return uuid;
    }

    @Override
    public String formatTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
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