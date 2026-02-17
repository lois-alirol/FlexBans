package fr.neocle.flexbans.web.cache;

import java.util.*;
import java.util.stream.Collectors;

import fr.neocle.flexbans.util.DateCalculator;
import fr.neocle.flexbans.util.PunishmentIdGenerator;

public class PunishmentCache {
    private static final List<Map<String, Object>> cachedAllPunishments = new ArrayList<>();
    private static final Map<String, Map<String, Integer>> cachedUserStats = new HashMap<>();
    private static final Map<String, Integer> cachedGlobalCounts = new HashMap<>();

    // Unified global counter
    private static int nextGlobalId = 0;

    public static void addPunishment(String targetName, String senderName, String reason,
                                     long duration, String serverScope, String type) {
        Map<String, Object> punishment = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        // Get the next unique ID (Last DB ID + 1)
        int id = getNextId();
        String punishmentId = PunishmentIdGenerator.generateId(type, id);

        punishment.put("id", id);
        punishment.put("database_id", id);
        punishment.put("punishment_id", punishmentId);
        punishment.put("type", type);
        punishment.put("player", targetName);
        punishment.put("moderator", senderName);
        punishment.put("reason", reason);
        punishment.put("status", "Active");
        punishment.put("date", DateCalculator.formatDate(currentTime));
        punishment.put("time_raw", currentTime);
        punishment.put("duration", DateCalculator.formatDuration(duration));
        punishment.put("server_scope", serverScope);

        cachedAllPunishments.addFirst(punishment);

        updateStats(senderName, targetName, type, "Active");
    }

    public static void removePunishment(int punishmentId) {
        for (Map<String, Object> punishment : cachedAllPunishments) {
            Object idObj = punishment.get("id");
            if (idObj instanceof Integer && (Integer) idObj == punishmentId) {
                punishment.put("status", "Removed");

                String moderator = (String) punishment.get("moderator");
                String player = (String) punishment.get("player");

                decrementActiveStats(moderator);
                decrementActiveStats(player);
                break;
            }
        }
    }

    public static List<Map<String, Object>> getRecentPunishments(int limit) {
        List<Map<String, Object>> recent = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> punishment : cachedAllPunishments) {
            if (count >= limit) break;
            recent.add(normalizePunishmentForOutput(punishment));
            count++;
        }
        return recent;
    }

    public static List<Map<String, Object>> getPunishmentsPage(int page, int pageSize) {
        if (page < 1 || pageSize <= 0) return Collections.emptyList();

        int startIndex = (page - 1) * pageSize;
        if (startIndex >= cachedAllPunishments.size()) return Collections.emptyList();

        int endIndex = Math.min(startIndex + pageSize, cachedAllPunishments.size());
        List<Map<String, Object>> pageResults = new ArrayList<>();

        for (int i = startIndex; i < endIndex; i++) {
            pageResults.add(normalizePunishmentForOutput(cachedAllPunishments.get(i)));
        }
        return pageResults;
    }

    public static Map<String, Map<String, Integer>> getUserStats() {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : cachedUserStats.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public static Map<String, Integer> getGlobalCounts() {
        return new HashMap<>(cachedGlobalCounts);
    }

    public static int getTotalPunishmentsCount() {
        return cachedAllPunishments.size();
    }

    public static List<Map<String, Object>> getAllPunishments() {
        return new ArrayList<>(cachedAllPunishments);
    }

    public static List<Map<String, Object>> getPunishmentsPageByType(int page, int pageSize, String type) {
        List<Map<String, Object>> filtered = cachedAllPunishments.stream()
                .filter(p -> type.equalsIgnoreCase((String) p.get("type")))
                .collect(Collectors.toList());

        int start = (page - 1) * pageSize;
        if (start >= filtered.size()) return Collections.emptyList();

        int end = Math.min(start + pageSize, filtered.size());
        return new ArrayList<>(filtered.subList(start, end));
    }

    public static int getTotalPunishmentsCountByType(String type) {
        return (int) cachedAllPunishments.stream()
                .filter(p -> type.equalsIgnoreCase((String) p.get("type")))
                .count();
    }

    public static void initialize(List<Map<String, Object>> allPunishments) {
        clear();

        cachedGlobalCounts.put("BAN", 0);
        cachedGlobalCounts.put("MUTE", 0);
        cachedGlobalCounts.put("WARNING", 0);
        cachedGlobalCounts.put("KICK", 0);

        // Find the maximum ID currently in the database to prevent overrides
        int maxId = 0;
        for (Map<String, Object> punishment : allPunishments) {
            Object idObj = punishment.get("id");
            if (idObj instanceof Number) {
                int currentId = ((Number) idObj).intValue();
                if (currentId > maxId) {
                    maxId = currentId;
                }
            }
        }
        // Set the counter to the last ID found
        nextGlobalId = maxId;

        cachedAllPunishments.addAll(allPunishments);

        for (Map<String, Object> punishment : allPunishments) {
            String type = (String) punishment.get("type");
            String moderator = (String) punishment.get("moderator");
            String player = (String) punishment.get("player");
            String status = (String) punishment.get("status");

            Object idObj = punishment.get("id");
            int idValue = (idObj instanceof Number) ? ((Number) idObj).intValue() : 0;

            String normalizedStatus = normalizeStatus(status);
            punishment.put("status", normalizedStatus);

            // Synchronize ID fields
            if (!punishment.containsKey("id")) punishment.put("id", idValue);
            if (!punishment.containsKey("database_id")) punishment.put("database_id", idValue);

            if (!punishment.containsKey("punishment_id")) {
                punishment.put("punishment_id", PunishmentIdGenerator.generateId(type, idValue));
            }

            rebuildStats(moderator, player, type, normalizedStatus);
            cachedGlobalCounts.put(type, cachedGlobalCounts.getOrDefault(type, 0) + 1);
        }
    }

    private static void updateStats(String sender, String target, String type, String status) {
        cachedUserStats.putIfAbsent(sender, new HashMap<>());
        Map<String, Integer> modStats = cachedUserStats.get(sender);
        modStats.put("sent", modStats.getOrDefault("sent", 0) + 1);
        if ("BAN".equals(type)) {
            modStats.put("bansSent", modStats.getOrDefault("bansSent", 0) + 1);
        }
        modStats.put("active", modStats.getOrDefault("active", 0) + 1);

        cachedUserStats.putIfAbsent(target, new HashMap<>());
        Map<String, Integer> playerStats = cachedUserStats.get(target);
        playerStats.put("received", playerStats.getOrDefault("received", 0) + 1);
        playerStats.put("active", playerStats.getOrDefault("active", 0) + 1);

        cachedGlobalCounts.put(type, cachedGlobalCounts.getOrDefault(type, 0) + 1);
    }

    private static void rebuildStats(String moderator, String player, String type, String status) {
        cachedUserStats.putIfAbsent(moderator, new HashMap<>());
        Map<String, Integer> modStats = cachedUserStats.get(moderator);

        modStats.put("sent", modStats.getOrDefault("sent", 0) + 1);
        if ("BAN".equals(type)) {
            modStats.put("bansSent", modStats.getOrDefault("bansSent", 0) + 1);
        }

        if ("Active".equals(status) || "Completed".equals(status)) {
            modStats.put("active", modStats.getOrDefault("active", 0) + 1);
        } else {
            modStats.put("removed", modStats.getOrDefault("removed", 0) + 1);
        }

        cachedUserStats.putIfAbsent(player, new HashMap<>());
        Map<String, Integer> playerStats = cachedUserStats.get(player);
        playerStats.put("received", playerStats.getOrDefault("received", 0) + 1);

        if ("Active".equals(status)) {
            playerStats.put("active", playerStats.getOrDefault("active", 0) + 1);
        } else if ("Expired".equals(status)) {
            playerStats.put("expired", playerStats.getOrDefault("expired", 0) + 1);
        } else {
            playerStats.put("removed", playerStats.getOrDefault("removed", 0) + 1);
        }
    }

    private static void decrementActiveStats(String user) {
        if (user != null && cachedUserStats.containsKey(user)) {
            Map<String, Integer> stats = cachedUserStats.get(user);
            stats.put("active", Math.max(0, stats.getOrDefault("active", 0) - 1));
            stats.put("removed", stats.getOrDefault("removed", 0) + 1);
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isEmpty()) return "Active";
        String normalized = status.toLowerCase().trim();
        return switch (normalized) {
            case "active" -> "Active";
            case "removed" -> "Removed";
            case "expired" -> "Expired";
            default -> status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        };
    }

    private static Map<String, Object> normalizePunishmentForOutput(Map<String, Object> punishment) {
        Map<String, Object> copy = new HashMap<>(punishment);
        if (!copy.containsKey("database_id") && copy.containsKey("id")) {
            copy.put("database_id", copy.get("id"));
        }
        if (!copy.containsKey("punishment_id")) {
            String type = (String) copy.get("type");
            Object idObj = copy.get("id");
            if (type != null && idObj instanceof Number) {
                copy.put("punishment_id", PunishmentIdGenerator.generateId(type, ((Number) idObj).intValue()));
            }
        }
        return copy;
    }

    private static synchronized int getNextId() {
        return ++nextGlobalId;
    }

    public static void clear() {
        cachedAllPunishments.clear();
        cachedUserStats.clear();
        cachedGlobalCounts.clear();
        nextGlobalId = 0;
    }
}