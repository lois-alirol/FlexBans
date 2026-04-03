package fr.neocle.flexbans.web.provider;

import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.DateCalculator;
import fr.neocle.flexbans.util.PunishmentIdGenerator;
import fr.neocle.flexbans.util.TextUtils;
import fr.neocle.flexbans.web.cache.PunishmentCache;
import fr.neocle.flexbans.web.loader.DatabasePunishmentLoader;

public class PunishmentDataProvider {
    private static PunishmentDataProvider instance;
    private final DatabaseUtils databaseUtils;
    private boolean initialized = false;

    private static final FlexLogger LOGGER = FlexLogger.get(PunishmentDataProvider.class);

    private enum PunishmentStatus {
        ACTIVE("Active"),
        REMOVED("Removed"),
        EXPIRED("Expired");

        private final String value;

        PunishmentStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String normalize(String status) {
            if (status == null || status.isEmpty()) {
                return ACTIVE.getValue();
            }
            String normalized = status.toLowerCase().trim();

            return switch (normalized) {
                case "active" -> ACTIVE.getValue();
                case "removed" -> REMOVED.getValue();
                case "expired" -> EXPIRED.getValue();
                default -> TextUtils.capitalize(status);
            };
        }
    }

    private PunishmentDataProvider(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
    }

    public static PunishmentDataProvider getInstance(DatabaseUtils databaseUtils) {
        if (instance == null) {
            instance = new PunishmentDataProvider(databaseUtils);
        }
        return instance;
    }

    public void initialize() {
        if (initialized) return;

        try {
            DatabasePunishmentLoader loader = new DatabasePunishmentLoader(databaseUtils);
            List<Map<String, Object>> allPunishments = loader.loadAllPunishments();

            PunishmentCache.initialize(allPunishments);
            initialized = true;

            LOGGER.info("Punishment data provider initialized");
        } catch (Exception e) {
            LOGGER.error("Error initializing punishment data provider: ", e);
        }
    }

    public Map<String, Object> getPunishmentsData() {
        Map<String, Object> data = new HashMap<>();
        data.put("recentPunishments", PunishmentCache.getRecentPunishments(25));
        data.put("userStats", PunishmentCache.getUserStats());
        data.put("globalCounts", PunishmentCache.getGlobalCounts());
        return data;
    }

    public List<Map<String, Object>> getPunishmentsPage(int page, int pageSize) {
        List<Map<String, Object>> rawList = PunishmentCache.getPunishmentsPage(page, pageSize);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Map<String, Object> punishment : rawList) {
            String hexId = (String) punishment.get("punishment_id");
            if (hexId != null && seenIds.contains(hexId)) continue;
            seenIds.add(hexId);

            Map<String, Object> cleaned = new HashMap<>(punishment);

            if (cleaned.containsKey("player")) {
                cleaned.remove("player_uuid");
            }
            result.add(cleaned);
        }
        return result;
    }

    public int getTotalPunishmentsCount() {
        return PunishmentCache.getTotalPunishmentsCount();
    }

    public Map<String, Integer> getGlobalCounts() {
        return PunishmentCache.getGlobalCounts();
    }

    public Object getPunishmentDetails(String hexId) throws Exception {
        if (hexId == null || hexId.isEmpty() || !PunishmentIdGenerator.validateId(hexId)) {
            LOGGER.warn("Invalid hex ID format: {}", hexId);
            return null;
        }

        List<Map<String, Object>> allPunishments = PunishmentCache.getAllPunishments();
        for (Map<String, Object> punishment : allPunishments) {
            String cachedHexId = (String) punishment.get("punishment_id");
            if (cachedHexId != null && cachedHexId.equals(hexId)) {
                return convertToDetailData(punishment);
            }
        }
        // If not found in cache, fetch from DB
        return fetchPunishmentDetailsByHexIdFromDatabase(hexId);
    }

    // Ajouter ces deux nouvelles méthodes à la classe PunishmentDataProvider:

    public List<Map<String, Object>> getPunishmentsPageByType(int page, int pageSize, String type) {
        List<Map<String, Object>> rawList = PunishmentCache.getPunishmentsPageByType(page, pageSize, type);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Map<String, Object> punishment : rawList) {
            String hexId = (String) punishment.get("punishment_id");
            if (hexId != null && seenIds.contains(hexId)) continue;
            seenIds.add(hexId);

            Map<String, Object> cleaned = new HashMap<>(punishment);
            if (cleaned.containsKey("player")) {
                cleaned.remove("player_uuid");
            }
            result.add(cleaned);
        }
        return result;
    }

    public int getTotalPunishmentsCountByType(String type) {
        return PunishmentCache.getTotalPunishmentsCountByType(type);
    }

    /**
     * Fetches punishment details using the new schema (central table + actors)
     * This now scans the table for the matching generated hexId.
     */
    private Object fetchPunishmentDetailsByHexIdFromDatabase(String hexId) throws Exception {
        String sql = """
            SELECT p.*,
                   pn.mc_username AS player_name,
                   issuer.type AS issuer_type, issuer.name AS issuer_name, issuer.player_uuid AS issuer_uuid,
                   pa.reason AS removal_reason, pa.action_time AS removal_time,
                   removed.type AS remover_type, removed.name AS remover_name, removed.player_uuid AS remover_uuid
            FROM punishments p
            LEFT JOIN player_names pn ON pn.player_uuid = p.target_uuid AND pn.last_seen = (
                SELECT MAX(last_seen) FROM player_names WHERE player_uuid = p.target_uuid
            )
            LEFT JOIN actors issuer ON issuer.id = p.issuer_actor_id
            LEFT JOIN punishment_actions pa ON pa.punishment_id = p.id AND pa.action = 'REMOVED'
            LEFT JOIN actors removed ON removed.id = pa.actor_id
            """;

        try (Connection conn = databaseUtils.getDatabaseConnectionManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String generatedHexId = PunishmentIdGenerator.generateId(type, id);
                if (generatedHexId.equals(hexId)) {
                    return mapResultSetToPunishmentInfo(rs, type, id);
                }
            }
        }

        LOGGER.warn("Punishment not found with hex ID: {}", hexId);
        return null;
    }

    private Object mapResultSetToPunishmentInfo(ResultSet rs, String type, int id) throws Exception {
        UUID targetUuid = rs.getString("target_uuid") != null && !rs.getString("target_uuid").isEmpty()
                ? UUID.fromString(rs.getString("target_uuid")) : null;
        int issuerActorId = rs.getInt("issuer_actor_id");
        String reason = rs.getString("reason");
        long createdAt = rs.getLong("created_at");
        long duration = rs.getLong("duration");
        String serverScope = rs.getString("server_scope");
        boolean ipScope = rs.getBoolean("ip_scope");

        // Wrap into a Map (for web response), prefer username for "player"
        Map<String, Object> details = new HashMap<>();
        details.put("database_id", id);
        details.put("punishment_id", PunishmentIdGenerator.generateId(type.toUpperCase(), id));
        details.put("punishment_type", type.toUpperCase());

        // Username if possible
        String playerName = rs.getString("player_name");
        if (playerName != null && !playerName.isEmpty()) {
            details.put("player", playerName);
        } else {
            details.put("player", targetUuid != null ? targetUuid.toString() : "Unknown");
        }

        details.put("issuer_actor_id", issuerActorId);
        details.put("reason", reason);
        details.put("execution_date", DateCalculator.formatTimestamp(createdAt));

        long expiresAt = rs.getObject("expires_at") != null ? rs.getLong("expires_at") : 0L;
        if (expiresAt > 0) {
            details.put("duration", DateCalculator.formatDuration(expiresAt - createdAt));
            details.put("expiration_date", DateCalculator.formatTimestamp(expiresAt));
        } else if (duration == -1) {
            details.put("duration", "Permanent");
            details.put("expiration_date", "Never");
        } else {
            details.put("duration", "Temporary");
            details.put("expiration_date", "Completed");
        }

        details.put("origin_server", rs.getString("server_origin"));
        details.put("scope_server", rs.getString("server_scope"));
        details.put("ip_scope", ipScope);

        // Actor info (issuer and remover, if any)
        details.put("executor", rs.getString("issuer_name"));

        String rawStatus = rs.getString("status");
        String normalizedStatus = PunishmentStatus.normalize(rawStatus);
        details.put("status", normalizedStatus);

        details.put("remover_name", rs.getString("remover_name"));
        details.put("removal_reason", rs.getString("removal_reason"));

        if (rs.getObject("removal_time") != null) {
            details.put("removal_time", DateCalculator.formatTimestamp(rs.getLong("removal_time")));
        }

        return details;
    }

    private Object convertToDetailData(Map<String, Object> punishment) {
        Map<String, Object> details = new HashMap<>();

        long id = ((Number) punishment.get("id")).longValue(); // <--- FIXED LINE
        String type = (String) punishment.get("type");
        String hexId = (String) punishment.get("punishment_id");
        if (hexId == null) {
            hexId = PunishmentIdGenerator.generateId(type, id);
        }

        details.put("database_id", id);
        details.put("punishment_id", hexId);

        details.put("punishment_type", punishment.get("type"));
        details.put("player", punishment.get("player")); // username, not uuid
        details.put("reason", punishment.get("reason"));
        details.put("execution_date", punishment.get("date"));
        details.put("duration", punishment.get("duration"));
        details.put("expiration_date", punishment.get("expiration_date") != null ?
                punishment.get("expiration_date") : "Never");
        details.put("origin_server", punishment.get("origin_server"));
        details.put("scope_server", punishment.get("server_scope"));
        details.put("executor", punishment.get("moderator"));

        String rawStatus = (String) punishment.get("status");
        String normalizedStatus = PunishmentStatus.normalize(rawStatus);
        details.put("status", normalizedStatus);

        details.put("remover_name", punishment.get("remover_name"));
        details.put("removal_reason", punishment.get("removal_reason"));

        if (punishment.containsKey("ip_scope")) {
            details.put("ip_scope", punishment.get("ip_scope"));
        }

        return details;
    }

    public void addPunishment(String targetName, String senderName, String reason,
                              long duration, String serverScope, String type) {
        PunishmentCache.addPunishment(targetName, senderName, reason, duration, serverScope, type);
    }

    public void removePunishment(int punishmentId) {
        PunishmentCache.removePunishment(punishmentId);
    }
}