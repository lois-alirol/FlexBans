package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.history. HistoryEntry;
import fr. neocle.flexbans. common.command.lookup.history. IHistoryCommandHelper;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database. DatabaseUtils;
import fr.neocle.flexbans.database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityHistoryCommandHelper implements IHistoryCommandHelper {
    private final ProxyServer proxyServer;
    private final ProfilesManager profilesManager;
    private final DatabaseConnectionManager dbManager;
    private final SimpleDateFormat dateFormat;

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
            return optPlayer.get(). getUniqueId();
        }

        UUID uuid = profilesManager.getUuid(playerName);
        if (uuid != null) {
            FlexLogger.info("[History] Player '" + playerName + "' found in database: " + uuid);
        }
        return uuid;
    }

    @Override
    public List<HistoryEntry> getPlayerHistory(UUID playerUuid) {
        List<HistoryEntry> entries = new ArrayList<>();

        entries.addAll(getBanHistory(playerUuid));
        entries.addAll(getMuteHistory(playerUuid));
        entries.addAll(getWarningHistory(playerUuid));
        entries.addAll(getKickHistory(playerUuid));
        entries.sort((a, b) -> Long.compare(b.time(), a.time()));

        return entries;
    }

    private List<HistoryEntry> getBanHistory(UUID playerUuid) {
        List<HistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, issuer_name, reason, time, status, removal_reason, ip_scope " +
                "FROM flexbans_bans WHERE target_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String type = ipScope ? "ip-ban" : "ban";

                    entries.add(new HistoryEntry(
                            rs.getInt("id"),
                            type,
                            rs. getString("issuer_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs.getString("status"),
                            rs. getString("removal_reason"),
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[History] Failed to fetch ban history for " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<HistoryEntry> getMuteHistory(UUID playerUuid) {
        List<HistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, issuer_name, reason, time, status, removal_reason, ip_scope " +
                "FROM flexbans_mutes WHERE target_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs. getBoolean("ip_scope");
                    String type = ipScope ? "ip-mute" : "mute";

                    entries.add(new HistoryEntry(
                            rs.getInt("id"),
                            type,
                            rs. getString("issuer_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs.getString("status"),
                            rs. getString("removal_reason"),
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[History] Failed to fetch mute history for " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<HistoryEntry> getWarningHistory(UUID playerUuid) {
        List<HistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, issuer_name, reason, time, status, removal_reason, ip_scope " +
                "FROM flexbans_warnings WHERE target_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt. setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String type = ipScope ? "ip-warning" : "warning";

                    entries.add(new HistoryEntry(
                            rs.getInt("id"),
                            type,
                            rs.getString("issuer_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs. getString("status"),
                            rs.getString("removal_reason"),
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[History] Failed to fetch warning history for " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<HistoryEntry> getKickHistory(UUID playerUuid) {
        List<HistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, issuer_name, reason, time, ip_scope FROM flexbans_kicks WHERE target_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs. next()) {
                    entries.add(new HistoryEntry(
                            rs.getInt("id"),
                            "kick",
                            rs.getString("issuer_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            "completed",
                            null,
                            rs.getBoolean("ip_scope")
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[History] Failed to fetch kick history for " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    @Override
    public String formatTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    @Override
    public List<String> getOnlinePlayerSuggestions(String partialName) {
        List<String> suggestions = new ArrayList<>();

        for (Player player : proxyServer. getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(partialName)) {
                suggestions.add(player.getUsername());
            }
        }

        return suggestions;
    }
}