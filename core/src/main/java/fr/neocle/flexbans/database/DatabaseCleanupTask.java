package fr.neocle.flexbans.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DatabaseCleanupTask {
    private final DatabaseConnectionManager dbManager;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DatabaseCleanupTask(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    public void startSessionCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            deleteOldSessions();
        }, 0, 30, TimeUnit.MINUTES);
    }

    public void deleteOldSessions() {
        String sql = "DELETE FROM sessions WHERE session_date < ?";
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(14);

        try (Connection connection = dbManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            Timestamp thresholdTimestamp = Timestamp.valueOf(thresholdDate);

            preparedStatement.setTimestamp(1, thresholdTimestamp);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
