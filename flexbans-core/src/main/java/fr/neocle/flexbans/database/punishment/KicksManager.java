package fr.neocle.flexbans.database.punishment;

import fr.neocle.flexbans.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class KicksManager {
    private final DatabaseConnectionManager dbManager;
    private final Logger logger;

    public KicksManager(DatabaseConnectionManager dbManager, Logger logger) {
        this.dbManager = dbManager;
        this.logger = logger;
    }

    public void insertKick(UUID targetUUID, String targetUsername, UUID issuerUUID, String issuerUsername,
                           String reason, String serverOrigin, boolean silent, boolean ipScope) {
        try (Connection connection = dbManager.getConnection()) {
            String query = "INSERT INTO flexbans_kicks (target_uuid, target_name, issuer_uuid, issuer_name, " +
                    "reason, time, server_origin, silent, ip_scope) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, targetUUID.toString());
                statement.setString(2, targetUsername);
                statement.setString(3, issuerUUID.toString());
                statement.setString(4, issuerUsername);
                statement.setString(5, reason);
                statement.setLong(6, System.currentTimeMillis());
                statement.setString(7, serverOrigin);
                statement.setBoolean(8, silent);
                statement.setBoolean(9, ipScope);

                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Failed to insert kick record");
            e.printStackTrace();
        }
    }
}
