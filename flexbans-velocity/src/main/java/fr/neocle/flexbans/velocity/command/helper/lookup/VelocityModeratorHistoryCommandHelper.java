package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.moderatorhistory.IModeratorHistoryCommandHelper;
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

public class VelocityModeratorHistoryCommandHelper implements IModeratorHistoryCommandHelper {
    private final ProxyServer proxyServer;
    private final ProfilesManager profilesManager;
    private final DatabaseConnectionManager dbManager;
    private final SimpleDateFormat dateFormat;

    private static final FlexLogger LOGGER = FlexLogger.get(VelocityModeratorHistoryCommandHelper.class);

    public VelocityModeratorHistoryCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                                                 DatabaseConnectionManager dbManager) {
        this.proxyServer = proxyServer;
        this.profilesManager = databaseUtils.getProfilesManager();
        this.dbManager = dbManager;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    }

    @Override
    public UUID getModeratorUuid(String moderatorName) {
        Optional<Player> optPlayer = proxyServer.getPlayer(moderatorName);
        if (optPlayer.isPresent()) {
            return optPlayer.get().getUniqueId();
        }

        UUID uuid = null;
        try {
            uuid = profilesManager.getUuid(moderatorName).join();
        } catch (Exception e) {
            LOGGER.error("Failed to resolve moderator UUID from database for '{}'", moderatorName, e);
        }

        if (uuid != null) {
            LOGGER.debug("Moderator '{}' found in database: {}", moderatorName, uuid);
        }
        return uuid;
    }

    @Override
    public List<ProfilesManager.ModeratorHistoryEntry> getModeratorHistory(UUID moderatorUuid) {
        List<ProfilesManager.ModeratorHistoryEntry> entries = new ArrayList<>();

        try {
            List<ProfilesManager.ModeratorHistoryEntry> issued =
                    profilesManager.getIssuedPunishments(moderatorUuid).join();
            if (issued != null) {
                entries.addAll(issued);
            }

            List<ProfilesManager.ModeratorHistoryEntry> removed =
                    profilesManager.getRemovedPunishments(moderatorUuid).join();
            if (removed != null) {
                entries.addAll(removed);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load moderator history for {}", moderatorUuid, e);
        }

        entries.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));
        return entries;
    }

    @Override
    public String formatTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    @Override
    public List<String> getOnlineModerators(String partialName) {
        List<String> suggestions = new ArrayList<>();

        for (Player player : proxyServer.getAllPlayers()) {
            if (player.hasPermission("flexbans.command.moderatorhistory") &&
                    player.getUsername().toLowerCase().startsWith(partialName.toLowerCase())) {
                suggestions.add(player.getUsername());
            }
        }

        return suggestions;
    }
}