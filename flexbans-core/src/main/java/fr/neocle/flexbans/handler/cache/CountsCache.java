package fr.neocle.flexbans.handler.cache;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.query.DashboardQueries;
import litebans.api.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CountsCache {
    public static int bansCount;
    public static int mutesCount;
    public static int kicksCount;
    public static int warningsCount;

    public static long lastUpdateTime;

    public static synchronized void update(DatabaseUtils db, boolean usingFlexBans, boolean usingLiteBans) {
        try {
            bansCount = getPunishmentCount(db, DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "bans"), usingFlexBans, usingLiteBans);
            mutesCount = getPunishmentCount(db, DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "mutes"), usingFlexBans, usingLiteBans);
            kicksCount = getPunishmentCount(db, DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "kicks"), usingFlexBans, usingLiteBans);
            warningsCount = getPunishmentCount(db, DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "warnings"), usingFlexBans, usingLiteBans);

            lastUpdateTime = System.currentTimeMillis();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update punishment counts", e);
        }
    }

    private static int getPunishmentCount(DatabaseUtils db, String table, boolean usingFlexBans, boolean usingLiteBans) throws SQLException {
        String query = DashboardQueries.getCountPunishmentsQuery(usingFlexBans, table);
        try (PreparedStatement stmt = usingFlexBans ? db.prepareStatement(query) : Database.get().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
