package fr.neocle.flexbans.database;

import java.util.Map;

public class DatabaseQueries {
    private static final Map<String, String> CREATE_USERS_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS users (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(255) UNIQUE,
                            discord_id VARCHAR(255) UNIQUE,
                            verification_code VARCHAR(255),
                            is_verified BOOLEAN DEFAULT FALSE,
                            password VARCHAR(255)
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS users (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            username TEXT UNIQUE,
                            discord_id TEXT UNIQUE,
                            verification_code TEXT,
                            is_verified BOOLEAN DEFAULT FALSE,
                            password TEXT
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS users (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(255) UNIQUE,
                            discord_id VARCHAR(255) UNIQUE,
                            verification_code VARCHAR(255),
                            is_verified BOOLEAN DEFAULT FALSE,
                            password VARCHAR(255)
                        );
                    """
    );

    private static final Map<String, String> CREATE_SESSIONS_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS sessions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            session_id VARCHAR(255) UNIQUE NOT NULL,
                            session_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            user_id INT NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS sessions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            session_id TEXT UNIQUE NOT NULL,
                            session_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            user_id INTEGER NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS sessions (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            session_id VARCHAR(255) UNIQUE NOT NULL,
                            session_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            user_id INT NOT NULL,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                    """
    );

    private static final Map<String, String> CREATE_HISTORY_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS flexbans_history (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            player_uuid VARCHAR(255) NOT NULL,
                            player_name VARCHAR(255) NOT NULL,
                            ip VARCHAR(255) NOT NULL
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS flexbans_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            ip TEXT NOT NULL
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS flexbans_history (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            player_uuid VARCHAR(255) NOT NULL,
                            player_name VARCHAR(255) NOT NULL,
                            ip VARCHAR(255) NOT NULL
                        );
                    """
    );

    private static final Map<String, String> CREATE_BANS_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS flexbans_bans (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_reason VARCHAR(255),
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) DEFAULT 'global' NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status VARCHAR(255) DEFAULT 'active' NOT NULL
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS flexbans_bans (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid TEXT NOT NULL,
                            target_name TEXT NOT NULL,
                            issuer_uuid TEXT NOT NULL,
                            issuer_name TEXT NOT NULL,
                            remover_uuid TEXT,
                            remover_name TEXT,
                            removal_reason TEXT,
                            reason TEXT NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope TEXT DEFAULT 'global' NOT NULL,
                            server_origin TEXT NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status TEXT DEFAULT 'active' NOT NULL
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS flexbans_bans (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_reason VARCHAR(255),
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) DEFAULT 'global' NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status VARCHAR(255) DEFAULT 'active' NOT NULL
                        );
                    """
    );

    private static final Map<String, String> CREATE_KICKS_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS flexbans_kicks (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS flexbans_kicks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid TEXT NOT NULL,
                            target_name TEXT NOT NULL,
                            issuer_uuid TEXT NOT NULL,
                            issuer_name TEXT NOT NULL,
                            reason TEXT NOT NULL,
                            time BIGINT NOT NULL,
                            server_origin TEXT NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS flexbans_kicks (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL
                        );
                    """
    );

    public static String getCreateTableQuery(String dbType, String tableName) {
        return switch (tableName.toLowerCase()) {
            case "users" -> CREATE_USERS_TABLE.get(dbType.toLowerCase());
            case "sessions" -> CREATE_SESSIONS_TABLE.get(dbType.toLowerCase());
            case "history" -> CREATE_HISTORY_TABLE.get(dbType.toLowerCase());
            case "bans" -> CREATE_BANS_TABLE.get(dbType.toLowerCase());
            case "kicks" -> CREATE_KICKS_TABLE.get(dbType.toLowerCase());
            default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };
    }
}
