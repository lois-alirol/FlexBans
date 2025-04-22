package fr.neocle.flexbans.database.queries;

public class DashboardQueries {
    public static final String LITEBANS_COUNT_PUNISHMENTS = "SELECT COUNT(*) FROM %s";

    public static final String LITEBANS_GET_PUNISHMENTS = "SELECT * FROM %s WHERE 1=1 %s ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String LITEBANS_COUNT_FILTERED_PUNISHMENTS = "SELECT COUNT(*) FROM %s WHERE 1=1 %s";

    public static final String LITEBANS_PLAYER_UUID_FILTER = " AND uuid = ?";
    public static final String LITEBANS_EXECUTOR_NAME_FILTER = " AND banned_by_name = ?";
    public static final String LITEBANS_ACTIVE_STATUS_FILTER = " AND (removed_by_name IS NULL OR removed_by_name = '') AND (until = -1 OR until = 0 OR until > ?)";
    public static final String LITEBANS_EXPIRED_STATUS_FILTER = " AND (removed_by_name = '#expired' OR (until > 0 AND until < ?))";
    public static final String LITEBANS_REMOVED_STATUS_FILTER = " AND (removed_by_name IS NOT NULL AND removed_by_name <> '#expired')";
    public static final String LITEBANS_DATE_ON_FILTER = " AND time >= ? AND time < ?";
    public static final String LITEBANS_DATE_BEFORE_FILTER = " AND time < ?";
    public static final String LITEBANS_DATE_AFTER_FILTER = " AND time > ?";

    public static final String LITEBANS_UUID_COLUMN = "uuid";
    public static final String LITEBANS_EXECUTOR_NAME_COLUMN = "banned_by_name";
    public static final String LITEBANS_REASON_COLUMN = "reason";
    public static final String LITEBANS_ID_COLUMN = "id";
    public static final String LITEBANS_TIME_COLUMN = "time";
    public static final String LITEBANS_REMOVED_BY_NAME_COLUMN = "removed_by_name";
    public static final String LITEBANS_REMOVED_BY_DATE_COLUMN = "removed_by_date";

    public static final String LITEBANS_MODERATOR_COUNT_QUERY_TEMPLATE =
            "SELECT COUNT(*) FROM (%s) AS t";

    public static final String LITEBANS_MODERATOR_GET_PUNISHMENTS_QUERY_TEMPLATE =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, type FROM (%s) AS t ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String LITEBANS_MODERATOR_BANS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, 'Ban' AS type FROM litebans_bans WHERE banned_by_name = ? %s";

    public static final String LITEBANS_MODERATOR_MUTES_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, 'Mute' AS type FROM litebans_mutes WHERE banned_by_name = ? %s";

    public static final String LITEBANS_MODERATOR_WARNINGS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, 'Warning' AS type FROM litebans_warnings WHERE banned_by_name = ? %s";

    public static final String LITEBANS_MODERATOR_KICKS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, 'Kick' AS type FROM litebans_kicks WHERE banned_by_name = ? %s";

    public static final String LITEBANS_PLAYER_COUNT_QUERY_TEMPLATE =
            "SELECT COUNT(*) FROM (%s) AS t";

    public static final String LITEBANS_PLAYER_GET_PUNISHMENTS_QUERY_TEMPLATE =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, removed_by_name, type FROM (%s) AS t ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String LITEBANS_PLAYER_BANS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, removed_by_name, 'Ban' AS type FROM litebans_bans WHERE uuid = ? %s";

    public static final String LITEBANS_PLAYER_MUTES_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, until, removed_by_name, 'Mute' AS type FROM litebans_mutes WHERE uuid = ? %s";

    public static final String LITEBANS_PLAYER_WARNINGS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, NULL AS removed_by_name, 'Warning' AS type FROM litebans_warnings WHERE uuid = ? %s";

    public static final String LITEBANS_PLAYER_KICKS_SUBQUERY =
            "SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, NULL AS removed_by_name, 'Kick' AS type FROM litebans_kicks WHERE uuid = ? %s";

    public static final String LITEBANS_PUNISHMENT_DETAILS_QUERY =
            "SELECT uuid, ipban, reason, banned_by_uuid, banned_by_name, removed_by_uuid, removed_by_name, " +
                    "removed_by_reason, removed_by_date, time, until, server_origin, active " +
                    "FROM %s WHERE id = ?";


    public static final String FLEXBANS_COUNT_PUNISHMENTS = "SELECT COUNT(*) FROM %s";

    public static final String FLEXBANS_GET_PUNISHMENTS = "SELECT * FROM %s WHERE 1=1 %s ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String FLEXBANS_COUNT_FILTERED_PUNISHMENTS = "SELECT COUNT(*) FROM %s WHERE 1=1 %s";

    public static final String FLEXBANS_PLAYER_UUID_FILTER = " AND target_uuid = ?";
    public static final String FLEXBANS_EXECUTOR_NAME_FILTER = " AND issuer_name = ?";
    public static final String FLEXBANS_ACTIVE_STATUS_FILTER = " AND status = 'active'";
    public static final String FLEXBANS_EXPIRED_STATUS_FILTER = " AND status = 'expired'";
    public static final String FLEXBANS_REMOVED_STATUS_FILTER = " AND status = 'removed'";
    public static final String FLEXBANS_DATE_ON_FILTER = " AND time >= ? AND time < ?";
    public static final String FLEXBANS_DATE_BEFORE_FILTER = " AND time < ?";
    public static final String FLEXBANS_DATE_AFTER_FILTER = " AND time > ?";

    public static final String FLEXBANS_UUID_COLUMN = "target_uuid";
    public static final String FLEXBANS_EXECUTOR_NAME_COLUMN = "issuer_name";
    public static final String FLEXBANS_REASON_COLUMN = "reason";
    public static final String FLEXBANS_ID_COLUMN = "id";
    public static final String FLEXBANS_TIME_COLUMN = "time";
    public static final String FLEXBANS_REMOVED_BY_NAME_COLUMN = "remover_name";
    public static final String FLEXBANS_REMOVED_BY_DATE_COLUMN = "removal_time";

    public static final String FLEXBANS_MODERATOR_COUNT_QUERY_TEMPLATE =
            "SELECT COUNT(*) FROM (%s) AS t";

    public static final String FLEXBANS_MODERATOR_GET_PUNISHMENTS_QUERY_TEMPLATE =
            "SELECT id, target_uuid AS uuid, reason, issuer_name AS banned_by_name, 0 AS ipban, time, " +
                    "CASE WHEN duration = -1 THEN -1 ELSE time + duration END AS until, " +
                    "type FROM (%s) AS t ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String FLEXBANS_MODERATOR_BANS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, 'Ban' AS type FROM flexbans_bans WHERE issuer_name = ? %s";

    public static final String FLEXBANS_MODERATOR_MUTES_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, 'Mute' AS type FROM flexbans_mutes WHERE issuer_name = ? %s";

    public static final String FLEXBANS_MODERATOR_WARNINGS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, 'Warning' AS type FROM flexbans_warnings WHERE issuer_name = ? %s";

    public static final String FLEXBANS_MODERATOR_KICKS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, 0 AS duration, 'Kick' AS type FROM flexbans_kicks WHERE issuer_name = ? %s";

    public static final String FLEXBANS_PLAYER_COUNT_QUERY_TEMPLATE =
            "SELECT COUNT(*) FROM (%s) AS t";

    public static final String FLEXBANS_PLAYER_GET_PUNISHMENTS_QUERY_TEMPLATE =
            "SELECT id, target_uuid AS uuid, reason, issuer_name AS banned_by_name, 0 AS ipban, time, " +
                    "CASE WHEN duration = -1 THEN -1 ELSE time + duration END AS until, " +
                    "remover_name AS removed_by_name, type FROM (%s) AS t ORDER BY time DESC LIMIT ? OFFSET ?";

    public static final String FLEXBANS_PLAYER_BANS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, remover_name, 'Ban' AS type FROM flexbans_bans WHERE target_uuid = ? %s";

    public static final String FLEXBANS_PLAYER_MUTES_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, remover_name, 'Mute' AS type FROM flexbans_mutes WHERE target_uuid = ? %s";

    public static final String FLEXBANS_PLAYER_WARNINGS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, duration, remover_name, 'Warning' AS type FROM flexbans_warnings WHERE target_uuid = ? %s";

    public static final String FLEXBANS_PLAYER_KICKS_SUBQUERY =
            "SELECT id, target_uuid, reason, issuer_name, time, 0 AS duration, NULL AS remover_name, 'Kick' AS type FROM flexbans_kicks WHERE target_uuid = ? %s";

    public static final String FLEXBANS_PUNISHMENT_DETAILS_QUERY =
            "SELECT target_uuid AS uuid, ip_scope AS ipban, reason, issuer_uuid AS banned_by_uuid, issuer_name AS banned_by_name, remover_uuid AS removed_by_uuid, remover_name AS removed_by_name, " +
                    "removal_reason AS removed_by_reason, removal_time AS removed_by_date, time, CASE WHEN duration = -1 THEN -1 ELSE time + duration END AS until, server_origin, status " +
                    "FROM %s WHERE id = ?";


    public static String getCountPunishmentsQuery(boolean usingFlexBans, String tableName) {
        String queryTemplate = usingFlexBans ? FLEXBANS_COUNT_PUNISHMENTS : LITEBANS_COUNT_PUNISHMENTS;
        return String.format(queryTemplate, tableName);
    }

    public static String buildWhereClause(boolean usingFlexBans, String player, String executor,
                                          String status, String on, String before, String after) {
        StringBuilder whereClause = new StringBuilder();

        if (player != null && !player.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_PLAYER_UUID_FILTER : LITEBANS_PLAYER_UUID_FILTER);
        }

        if (executor != null && !executor.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_EXECUTOR_NAME_FILTER : LITEBANS_EXECUTOR_NAME_FILTER);
        }

        if (status != null && !status.isEmpty()) {
            switch (status.toLowerCase()) {
                case "active":
                    whereClause.append(usingFlexBans ? FLEXBANS_ACTIVE_STATUS_FILTER : LITEBANS_ACTIVE_STATUS_FILTER);
                    break;
                case "expired":
                    whereClause.append(usingFlexBans ? FLEXBANS_EXPIRED_STATUS_FILTER : LITEBANS_EXPIRED_STATUS_FILTER);
                    break;
                case "removed":
                    whereClause.append(usingFlexBans ? FLEXBANS_REMOVED_STATUS_FILTER : LITEBANS_REMOVED_STATUS_FILTER);
                    break;
            }
        }

        if (on != null && !on.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_DATE_ON_FILTER : LITEBANS_DATE_ON_FILTER);
        }

        if (before != null && !before.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_DATE_BEFORE_FILTER : LITEBANS_DATE_BEFORE_FILTER);
        }

        if (after != null && !after.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_DATE_AFTER_FILTER : LITEBANS_DATE_AFTER_FILTER);
        }

        return whereClause.toString();
    }

    public static String buildPunishmentsQuery(boolean usingFlexBans, String tableName,
                                               String player, String executor, String status,
                                               String on, String before, String after,
                                               int pageSize, int offset) {
        String whereClause = buildWhereClause(usingFlexBans, player, executor, status, on, before, after);
        String queryTemplate = usingFlexBans ? FLEXBANS_GET_PUNISHMENTS : LITEBANS_GET_PUNISHMENTS;
        return String.format(queryTemplate, tableName, whereClause);
    }

    public static String buildCountFilteredPunishmentsQuery(boolean usingFlexBans, String tableName,
                                                            String player, String executor, String status,
                                                            String on, String before, String after) {
        String whereClause = buildWhereClause(usingFlexBans, player, executor, status, on, before, after);
        String queryTemplate = usingFlexBans ? FLEXBANS_COUNT_FILTERED_PUNISHMENTS : LITEBANS_COUNT_FILTERED_PUNISHMENTS;
        return String.format(queryTemplate, tableName, whereClause);
    }

    public static String getTableName(boolean usingFlexBans, boolean usingLiteBans, String type) {
        String prefix;

        if (usingFlexBans) {
            prefix = "flexbans_";
        } else if (usingLiteBans) {
            prefix = "litebans_";
        } else {
            prefix = "";
        }

        switch (type.toLowerCase()) {
            case "warnings":
                return prefix + "warnings";
            case "mutes":
                return prefix + "mutes";
            case "kicks":
                return prefix + "kicks";
            case "bans":
            default:
                return prefix + "bans";
        }
    }

    public static String getColumnName(boolean usingFlexBans, boolean usingLiteBans, String columnKey) {
        switch (columnKey) {
            case "targetUUID":
                return usingFlexBans ? FLEXBANS_UUID_COLUMN : LITEBANS_UUID_COLUMN;
            case "executor":
                return usingFlexBans ? FLEXBANS_EXECUTOR_NAME_COLUMN : LITEBANS_EXECUTOR_NAME_COLUMN;
            case "reason":
                return usingFlexBans ? FLEXBANS_REASON_COLUMN : LITEBANS_REASON_COLUMN;
            case "id":
                return usingFlexBans ? FLEXBANS_ID_COLUMN : LITEBANS_ID_COLUMN;
            case "time":
                return usingFlexBans ? FLEXBANS_TIME_COLUMN : LITEBANS_TIME_COLUMN;
            case "removed_by_name":
                return usingFlexBans ? FLEXBANS_REMOVED_BY_NAME_COLUMN : LITEBANS_REMOVED_BY_NAME_COLUMN;
            case "removed_by_date":
                return usingFlexBans ? FLEXBANS_REMOVED_BY_DATE_COLUMN : LITEBANS_REMOVED_BY_DATE_COLUMN;
            default:
                throw new IllegalArgumentException("Unknown column key: " + columnKey);
        }
    }

    public static String buildModeratorWhereClause(boolean usingFlexBans, String player) {
        StringBuilder whereClause = new StringBuilder();

        if (player != null && !player.isEmpty()) {
            whereClause.append(usingFlexBans ? FLEXBANS_PLAYER_UUID_FILTER : LITEBANS_PLAYER_UUID_FILTER);
        }

        return whereClause.toString();
    }

    public static String buildModeratorCountQuery(boolean usingFlexBans, String executor, String player) {
        String whereClause = buildModeratorWhereClause(usingFlexBans, player);

        StringBuilder unionQueries = new StringBuilder();

        if (usingFlexBans) {
            System.out.println("using flexbans");

            unionQueries.append(String.format(FLEXBANS_MODERATOR_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_KICKS_SUBQUERY, whereClause));

            return String.format(FLEXBANS_MODERATOR_COUNT_QUERY_TEMPLATE, unionQueries.toString());
        } else {
            unionQueries.append(String.format(LITEBANS_MODERATOR_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_KICKS_SUBQUERY, whereClause));

            return String.format(LITEBANS_MODERATOR_COUNT_QUERY_TEMPLATE, unionQueries.toString());
        }
    }

    public static String buildModeratorPunishmentsQuery(boolean usingFlexBans, String executor, String player, int pageSize, int offset) {
        String whereClause = buildModeratorWhereClause(usingFlexBans, player);

        StringBuilder unionQueries = new StringBuilder();

        if (usingFlexBans) {
            unionQueries.append(String.format(FLEXBANS_MODERATOR_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_MODERATOR_KICKS_SUBQUERY, whereClause));

            return String.format(FLEXBANS_MODERATOR_GET_PUNISHMENTS_QUERY_TEMPLATE, unionQueries.toString());
        } else {
            unionQueries.append(String.format(LITEBANS_MODERATOR_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_MODERATOR_KICKS_SUBQUERY, whereClause));

            return String.format(LITEBANS_MODERATOR_GET_PUNISHMENTS_QUERY_TEMPLATE, unionQueries.toString());
        }
    }

    public static String buildPlayerWhereClause(boolean usingFlexBans, String executor) {
        StringBuilder whereClause = new StringBuilder();

        if (executor != null && !executor.isEmpty()) {
            whereClause.append(usingFlexBans ? " AND issuer_name = ?" : " AND banned_by_name = ?");
        }

        return whereClause.toString();
    }

    public static String buildPlayerCountQuery(boolean usingFlexBans, String player, String executor) {
        String whereClause = buildPlayerWhereClause(usingFlexBans, executor);

        StringBuilder unionQueries = new StringBuilder();

        if (usingFlexBans) {
            unionQueries.append(String.format(FLEXBANS_PLAYER_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_KICKS_SUBQUERY, whereClause));

            return String.format(FLEXBANS_PLAYER_COUNT_QUERY_TEMPLATE, unionQueries.toString());
        } else {
            unionQueries.append(String.format(LITEBANS_PLAYER_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_KICKS_SUBQUERY, whereClause));

            return String.format(LITEBANS_PLAYER_COUNT_QUERY_TEMPLATE, unionQueries.toString());
        }
    }

    public static String buildPlayerPunishmentsQuery(boolean usingFlexBans, String player, String executor, int pageSize, int offset) {
        String whereClause = buildPlayerWhereClause(usingFlexBans, executor);

        StringBuilder unionQueries = new StringBuilder();

        if (usingFlexBans) {
            unionQueries.append(String.format(FLEXBANS_PLAYER_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(FLEXBANS_PLAYER_KICKS_SUBQUERY, whereClause));

            return String.format(FLEXBANS_PLAYER_GET_PUNISHMENTS_QUERY_TEMPLATE, unionQueries.toString());
        } else {
            unionQueries.append(String.format(LITEBANS_PLAYER_BANS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_MUTES_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_WARNINGS_SUBQUERY, whereClause));
            unionQueries.append(" UNION ALL ");
            unionQueries.append(String.format(LITEBANS_PLAYER_KICKS_SUBQUERY, whereClause));

            return String.format(LITEBANS_PLAYER_GET_PUNISHMENTS_QUERY_TEMPLATE, unionQueries.toString());
        }
    }

    public static String buildPunishmentDetailsQuery(boolean usingFlexBans, String tableName) {
        String queryTemplate = usingFlexBans ? FLEXBANS_PUNISHMENT_DETAILS_QUERY : LITEBANS_PUNISHMENT_DETAILS_QUERY;
        return String.format(queryTemplate, tableName);
    }
}