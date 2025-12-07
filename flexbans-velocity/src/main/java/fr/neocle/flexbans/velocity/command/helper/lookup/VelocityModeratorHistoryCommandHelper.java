package fr.neocle.flexbans.velocity.command.helper.lookup;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.moderatorhistory.IModeratorHistoryCommandHelper;
import fr.neocle. flexbans.common.command.lookup.moderatorhistory.ModeratorHistoryEntry;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr. neocle.flexbans. database.player.ProfilesManager;
import fr.neocle.flexbans.logger.FlexLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java. sql.SQLException;
import java. text.SimpleDateFormat;
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

    public VelocityModeratorHistoryCommandHelper(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                                                 DatabaseConnectionManager dbManager) {
        this.proxyServer = proxyServer;
        this. profilesManager = databaseUtils. getProfilesManager();
        this.dbManager = dbManager;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    }

    @Override
    public UUID getModeratorUuid(String moderatorName) {
        Optional<Player> optPlayer = proxyServer.getPlayer(moderatorName);
        if (optPlayer.isPresent()) {
            return optPlayer.get().getUniqueId();
        }

        UUID uuid = profilesManager.getUuid(moderatorName);
        if (uuid != null) {
            FlexLogger.info("[ModeratorHistory] Moderator '" + moderatorName + "' found in database: " + uuid);
        }
        return uuid;
    }

    @Override
    public List<ModeratorHistoryEntry> getModeratorHistory(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();

        entries.addAll(getIssuedBans(moderatorUuid));
        entries.addAll(getIssuedMutes(moderatorUuid));
        entries.addAll(getIssuedWarnings(moderatorUuid));
        entries.addAll(getIssuedKicks(moderatorUuid));
        entries.addAll(getRemovedPunishments(moderatorUuid));
        entries.sort((a, b) -> Long.compare(b.timestamp(), a.timestamp()));

        return entries;
    }

    private List<ModeratorHistoryEntry> getIssuedBans(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, target_name, reason, time, duration, status, ip_scope " +
                "FROM flexbans_bans WHERE issuer_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, moderatorUuid. toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String type = ipScope ? "ip-ban" : "ban";

                    entries.add(new ModeratorHistoryEntry(
                            rs. getInt("id"),
                            "issued",
                            type,
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs.getLong("duration"),
                            rs.getString("status"),
                            null,
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[ModeratorHistory] Failed to fetch issued bans for " + moderatorUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<ModeratorHistoryEntry> getIssuedMutes(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, target_name, reason, time, duration, status, ip_scope " +
                "FROM flexbans_mutes WHERE issuer_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, moderatorUuid.toString());

            try (ResultSet rs = stmt. executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String type = ipScope ? "ip-mute" : "mute";

                    entries.add(new ModeratorHistoryEntry(
                            rs.getInt("id"),
                            "issued",
                            type,
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs.getLong("duration"),
                            rs.getString("status"),
                            null,
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[ModeratorHistory] Failed to fetch issued mutes for " + moderatorUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<ModeratorHistoryEntry> getIssuedWarnings(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, target_name, reason, time, duration, status, ip_scope " +
                "FROM flexbans_warnings WHERE issuer_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection. prepareStatement(query)) {
            stmt.setString(1, moderatorUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String type = ipScope ?  "ip-warning" : "warning";

                    entries.add(new ModeratorHistoryEntry(
                            rs.getInt("id"),
                            "issued",
                            type,
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            rs. getLong("duration"),
                            rs.getString("status"),
                            null,
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[ModeratorHistory] Failed to fetch issued warnings for " + moderatorUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<ModeratorHistoryEntry> getIssuedKicks(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, target_name, reason, time, ip_scope FROM flexbans_kicks WHERE issuer_uuid = ? ORDER BY time DESC";

        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, moderatorUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ModeratorHistoryEntry(
                            rs. getInt("id"),
                            "issued",
                            "kick",
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("time"),
                            0,
                            "completed",
                            null,
                            rs.getBoolean("ip_scope")
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[ModeratorHistory] Failed to fetch issued kicks for " + moderatorUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    private List<ModeratorHistoryEntry> getRemovedPunishments(UUID moderatorUuid) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();

        // Get removed bans
        entries.addAll(getRemovedType(moderatorUuid, "flexbans_bans", "ban"));
        // Get removed mutes
        entries. addAll(getRemovedType(moderatorUuid, "flexbans_mutes", "mute"));
        // Get removed warnings
        entries.addAll(getRemovedType(moderatorUuid, "flexbans_warnings", "warning"));

        return entries;
    }

    private List<ModeratorHistoryEntry> getRemovedType(UUID moderatorUuid, String tableName, String type) {
        List<ModeratorHistoryEntry> entries = new ArrayList<>();
        String query = "SELECT id, target_name, removal_reason, removal_time, ip_scope " +
                "FROM " + tableName + " WHERE remover_uuid = ? AND removal_time IS NOT NULL ORDER BY removal_time DESC";

        try (Connection connection = dbManager. getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, moderatorUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    boolean ipScope = rs.getBoolean("ip_scope");
                    String fullType = ipScope ? "ip-" + type : type;

                    entries.add(new ModeratorHistoryEntry(
                            rs.getInt("id"),
                            "removed",
                            fullType,
                            rs.getString("target_name"),
                            rs.getString("removal_reason"),
                            rs.getLong("removal_time"),
                            0,
                            "removed",
                            null,
                            ipScope
                    ));
                }
            }
        } catch (SQLException e) {
            FlexLogger.warn("[ModeratorHistory] Failed to fetch removed " + type + "s for " + moderatorUuid + ": " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    @Override
    public String formatTime(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    @Override
    public List<String> getOnlineModerators(String partialName) {
        List<String> suggestions = new ArrayList<>();

        for (Player player : proxyServer. getAllPlayers()) {
            if (player.hasPermission("flexbans.command.moderatorhistory") &&
                    player.getUsername().toLowerCase().startsWith(partialName.toLowerCase())) {
                suggestions.add(player.getUsername());
            }
        }

        return suggestions;
    }
}