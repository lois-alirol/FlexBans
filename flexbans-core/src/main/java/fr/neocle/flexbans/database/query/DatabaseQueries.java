package fr.neocle.flexbans.database.query;

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
                            removal_time BIGINT,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) NOT NULL,
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
                            removal_time BIGINT,
                            reason TEXT NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope TEXT NOT NULL,
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
                            removal_time BIGINT,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) NOT NULL,
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

    private static final Map<String, String> CREATE_SERVER_LOCKS_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS flexbans_server_locks (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            server_name VARCHAR(255) NOT NULL,
                            start_time BIGINT NOT NULL,
                            reason VARCHAR(255) NOT NULL,
                            duration BIGINT NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_time BIGINT,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status TEXT DEFAULT 'locked' NOT NULL
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS flexbans_server_locks (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            server_name TEXT NOT NULL,
                            start_time BIGINT NOT NULL,
                            reason TEXT NOT NULL,
                            duration BIGINT NOT NULL,
                            issuer_uuid TEXT NOT NULL,
                            issuer_name TEXT NOT NULL,
                            remover_uuid TEXT,
                            remover_name TEXT,
                            removal_time BIGINT,
                            server_origin TEXT NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status TEXT DEFAULT 'locked' NOT NULL
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS flexbans_server_locks (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            server_name VARCHAR(255) NOT NULL,
                            start_time BIGINT NOT NULL,
                            reason VARCHAR(255) NOT NULL,
                            duration BIGINT NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_time BIGINT,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status TEXT DEFAULT 'locked' NOT NULL
                        );
                    """
    );

    private static final Map<String, String> CREATE_MUTES_TABLE = Map.of(
            "mysql", """
                        CREATE TABLE IF NOT EXISTS flexbans_mutes (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_reason VARCHAR(255),
                            removal_time BIGINT,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status VARCHAR(255) DEFAULT 'active' NOT NULL
                        );
                    """,
            "sqlite", """
                        CREATE TABLE IF NOT EXISTS flexbans_mutes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid TEXT NOT NULL,
                            target_name TEXT NOT NULL,
                            issuer_uuid TEXT NOT NULL,
                            issuer_name TEXT NOT NULL,
                            remover_uuid TEXT,
                            remover_name TEXT,
                            removal_reason TEXT,
                            removal_time BIGINT,
                            reason TEXT NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope TEXT NOT NULL,
                            server_origin TEXT NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status TEXT DEFAULT 'active' NOT NULL
                        );
                    """,
            "h2", """
                        CREATE TABLE IF NOT EXISTS flexbans_mutes (
                            id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                            ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                            target_uuid VARCHAR(255) NOT NULL,
                            target_name VARCHAR(255) NOT NULL,
                            issuer_uuid VARCHAR(255) NOT NULL,
                            issuer_name VARCHAR(255) NOT NULL,
                            remover_uuid VARCHAR(255),
                            remover_name VARCHAR(255),
                            removal_reason VARCHAR(255),
                            removal_time BIGINT,
                            reason VARCHAR(255) NOT NULL,
                            time BIGINT NOT NULL,
                            duration BIGINT NOT NULL,
                            server_scope VARCHAR(255) NOT NULL,
                            server_origin VARCHAR(255) NOT NULL,
                            silent BOOLEAN DEFAULT FALSE NOT NULL,
                            status VARCHAR(255) DEFAULT 'active' NOT NULL
                        );
                    """
    );

    private static final Map<String, String> CREATE_WARNINGS_TABLE = Map.of(
            "mysql", """
                    CREATE TABLE IF NOT EXISTS flexbans_warnings (
                        id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                        ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                        target_uuid VARCHAR(255) NOT NULL,
                        target_name VARCHAR(255) NOT NULL,
                        issuer_uuid VARCHAR(255) NOT NULL,
                        issuer_name VARCHAR(255) NOT NULL,
                        remover_uuid VARCHAR(255),
                        remover_name VARCHAR(255),
                        removal_reason VARCHAR(255),
                        removal_time BIGINT,
                        reason VARCHAR(255) NOT NULL,
                        time BIGINT NOT NULL,
                        duration BIGINT NOT NULL,
                        server_scope VARCHAR(255) NOT NULL,
                        server_origin VARCHAR(255) NOT NULL,
                        silent BOOLEAN DEFAULT FALSE NOT NULL,
                        status VARCHAR(255) DEFAULT 'active' NOT NULL
                    );
                """,
            "sqlite", """
                    CREATE TABLE IF NOT EXISTS flexbans_warnings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                        target_uuid TEXT NOT NULL,
                        target_name TEXT NOT NULL,
                        issuer_uuid TEXT NOT NULL,
                        issuer_name TEXT NOT NULL,
                        remover_uuid TEXT,
                        remover_name TEXT,
                        removal_reason TEXT,
                        removal_time BIGINT,
                        reason TEXT NOT NULL,
                        time BIGINT NOT NULL,
                        duration BIGINT NOT NULL,
                        server_scope TEXT NOT NULL,
                        server_origin TEXT NOT NULL,
                        silent BOOLEAN DEFAULT FALSE NOT NULL,
                        status TEXT DEFAULT 'active' NOT NULL
                    );
                """,
            "h2", """
                    CREATE TABLE IF NOT EXISTS flexbans_warnings (
                        id INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
                        ip_scope BOOLEAN DEFAULT FALSE NOT NULL,
                        target_uuid VARCHAR(255) NOT NULL,
                        target_name VARCHAR(255) NOT NULL,
                        issuer_uuid VARCHAR(255) NOT NULL,
                        issuer_name VARCHAR(255) NOT NULL,
                        remover_uuid VARCHAR(255),
                        remover_name VARCHAR(255),
                        removal_reason VARCHAR(255),
                        removal_time BIGINT,
                        reason VARCHAR(255) NOT NULL,
                        time BIGINT NOT NULL,
                        duration BIGINT NOT NULL,
                        server_scope VARCHAR(255) NOT NULL,
                        server_origin VARCHAR(255) NOT NULL,
                        silent BOOLEAN DEFAULT FALSE NOT NULL,
                        status VARCHAR(255) DEFAULT 'active' NOT NULL
                    );
                """
    );

    private static final Map<String, String> CREATE_PROFILES_TABLE = Map.of(
            "mysql", """
                    CREATE TABLE IF NOT EXISTS flexbans_profiles (
                        uuid VARCHAR(36) PRIMARY KEY,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,

            "sqlite", """
                    CREATE TABLE IF NOT EXISTS flexbans_profiles (
                        uuid TEXT PRIMARY KEY,
                        first_seen INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL
                    );
                """,

            "h2", """
                    CREATE TABLE IF NOT EXISTS flexbans_profiles (
                        uuid VARCHAR(36) PRIMARY KEY,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL
                    );
                """
    );

    private static final Map<String, String> CREATE_NAMES_TABLE = Map.of(
            "mysql", """
                    CREATE TABLE IF NOT EXISTS flexbans_names (
                        uuid VARCHAR(36) NOT NULL,
                        username VARCHAR(16) NOT NULL,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (uuid, username)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,

            "sqlite", """
                    CREATE TABLE IF NOT EXISTS flexbans_names (
                        uuid TEXT NOT NULL,
                        username TEXT NOT NULL CHECK (length(username) <= 16),
                        first_seen INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL,
                        PRIMARY KEY (uuid, username)
                    );
                """,

            "h2", """
                    CREATE TABLE IF NOT EXISTS flexbans_names (
                        uuid VARCHAR(36) NOT NULL,
                        username VARCHAR(16) NOT NULL,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (uuid, username)
                    );
                """
    );

    private static final Map<String, String> CREATE_IPS_TABLE = Map.of(
            "mysql", """
                    CREATE TABLE IF NOT EXISTS flexbans_ips (
                        uuid VARCHAR(36) NOT NULL,
                        ip BINARY(16) NOT NULL,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (uuid, ip)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """,

            "sqlite", """
                    CREATE TABLE IF NOT EXISTS flexbans_ips (
                        uuid TEXT NOT NULL,
                        ip BLOB NOT NULL,
                        first_seen INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL,
                        PRIMARY KEY (uuid, ip)
                    );
                """,
            "h2", """
                    CREATE TABLE IF NOT EXISTS flexbans_ips (
                        uuid VARCHAR(36) NOT NULL,
                        ip BINARY(16) NOT NULL,
                        first_seen BIGINT NOT NULL,
                        last_seen BIGINT NOT NULL,
                        PRIMARY KEY (uuid, ip)
                    );
                """
    );


    public static String getCreateTableQuery(String dbType, String tableName) {
        return switch (tableName.toLowerCase()) {
            case "users" -> CREATE_USERS_TABLE.get(dbType.toLowerCase());
            case "sessions" -> CREATE_SESSIONS_TABLE.get(dbType.toLowerCase());
            case "bans" -> CREATE_BANS_TABLE.get(dbType.toLowerCase());
            case "mutes" -> CREATE_MUTES_TABLE.get(dbType.toLowerCase());
            case "kicks" -> CREATE_KICKS_TABLE.get(dbType.toLowerCase());
            case "warnings" -> CREATE_WARNINGS_TABLE.get(dbType.toLowerCase());
            case "server_locks" -> CREATE_SERVER_LOCKS_TABLE.get(dbType.toLowerCase());
            case "profiles" -> CREATE_PROFILES_TABLE.get(dbType.toLowerCase());
            case "names" -> CREATE_NAMES_TABLE.get(dbType.toLowerCase());
            case "ips" -> CREATE_IPS_TABLE.get(dbType.toLowerCase());
            default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };
    }
}
