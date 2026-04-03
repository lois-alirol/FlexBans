package fr.neocle.flexbans.web.loader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.DateCalculator;
import fr.neocle.flexbans.util.PunishmentIdGenerator;

public class DatabasePunishmentLoader {
    private final DatabaseUtils databaseUtils;

    private static final FlexLogger LOGGER = FlexLogger.get(DatabasePunishmentLoader.class);

    public DatabasePunishmentLoader(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
    }

    public List<Map<String, Object>> loadAllPunishments() throws Exception {
        List<Map<String, Object>> allPunishments = new ArrayList<>();
        String sql = """
            SELECT
                   p.id AS punishment_id,
                   p.type,
                   p.target_uuid,
                   p.created_at,
                   p.reason,
                   p.duration,
                   p.expires_at,
                   p.server_scope,
                   p.server_origin,
                   p.status,
                   p.ip_scope,
                   pn.mc_username AS player_name,
                   issuer.name AS issuer_name,
                   issuer.type AS issuer_type,
                   removed_actor.name AS remover_name,
                   pa.reason AS removal_reason,
                   pa.action_time AS removal_time
            FROM punishments p
            LEFT JOIN player_names pn ON pn.player_uuid = p.target_uuid AND pn.last_seen = (
                SELECT MAX(last_seen) FROM player_names WHERE player_uuid = p.target_uuid
            )
            LEFT JOIN actors issuer ON issuer.id = p.issuer_actor_id
            LEFT JOIN punishment_actions pa ON pa.punishment_id = p.id AND pa.action = 'REMOVED'
            LEFT JOIN actors removed_actor ON removed_actor.id = pa.actor_id
            ORDER BY p.created_at DESC
        """;

        try (Connection connection = databaseUtils.getDatabaseConnectionManager().getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Map<String, Object> punishment = new LinkedHashMap<>();

                    long punishmentId = rs.getLong("punishment_id");
                    String type = rs.getString("type");

                    // Main identifiers
                    punishment.put("id", punishmentId);                 // Now correctly set
                    punishment.put("database_id", punishmentId);         // Now correctly set
                    punishment.put("punishment_id", PunishmentIdGenerator.generateId(type, punishmentId));
                    punishment.put("type", type);

                    // Only show player name (never UUID, unless you want a separate admin/debug endpoint)
                    String playerName = rs.getString("player_name");
                    if (playerName != null && !playerName.isEmpty()) {
                        punishment.put("player", playerName);
                    } else {
                        punishment.put("player", "Unknown");
                    }

                    // Moderator who issued
                    String moderator = rs.getString("issuer_name");
                    punishment.put("moderator", moderator != null ? moderator : "Console");

                    // Reason
                    punishment.put("reason", rs.getString("reason"));

                    // Date info
                    long createdAt = rs.getLong("created_at");
                    punishment.put("date", DateCalculator.formatDate(createdAt));
                    punishment.put("time_raw", createdAt);

                    // Scope/server
                    String serverScope = rs.getString("server_scope");
                    punishment.put("server_scope", serverScope != null ? serverScope : "Global");
                    punishment.put("origin_server", rs.getString("server_origin"));

                    // Status
                    String status = rs.getString("status");
                    punishment.put("status", status != null ? status : "Active");

                    // IP scope
                    punishment.put("ip_scope", rs.getBoolean("ip_scope"));

                    // Duration/Expiration
                    long duration = rs.getObject("duration") != null ? rs.getLong("duration") : 0L;
                    punishment.put("duration", DateCalculator.formatDuration(duration));

                    long expiresAt = rs.getObject("expires_at") != null ? rs.getLong("expires_at") : 0L;
                    if (expiresAt > 0) {
                        punishment.put("expiration_date", DateCalculator.formatDate(expiresAt));
                    } else if (duration == -1) {
                        punishment.put("expiration_date", "Never");
                    }

                    // Removal info
                    String removerName = rs.getString("remover_name");
                    if (removerName != null) punishment.put("remover_name", removerName);

                    String removalReason = rs.getString("removal_reason");
                    if (removalReason != null) punishment.put("removal_reason", removalReason);

                    if (rs.getObject("removal_time") != null) {
                        punishment.put("removal_time", DateCalculator.formatDate(rs.getLong("removal_time")));
                    }

                    allPunishments.add(punishment);
                } catch (Exception e) {
                    LOGGER.error("Error mapping punishment from table: ", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading punishments from central schema: ", e);
        }

        allPunishments.sort((a, b) -> Long.compare((Long) b.get("time_raw"), (Long) a.get("time_raw"))); // already ordered, but fine for safety
        return allPunishments;
    }
}