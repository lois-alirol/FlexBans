package fr.neocle.flexbans.database.query;

import java.util.Map;

public class DatabaseQueries {

    private static final Map<String, String> CREATE_PLAYERS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS players (
                    uuid CHAR(36) PRIMARY KEY,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    first_seen INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL
                );
            """
    );

    private static final Map<String, String> CREATE_PLAYER_NAMES_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS player_names (
                    player_uuid CHAR(36) NOT NULL,
                    mc_username VARCHAR(16) NOT NULL,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, mc_username),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS player_names (
                    player_uuid TEXT NOT NULL,
                    mc_username TEXT NOT NULL CHECK (length(mc_username) <= 16),
                    first_seen INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, mc_username),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS player_names (
                    player_uuid VARCHAR(36) NOT NULL,
                    mc_username VARCHAR(16) NOT NULL,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, mc_username),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                );
            """
    );

    private static final Map<String, String> CREATE_PLAYER_IPS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS player_ips (
                    player_uuid CHAR(36) NOT NULL,
                    ip BINARY(16) NOT NULL,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, ip),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS player_ips (
                    player_uuid TEXT NOT NULL,
                    ip BLOB NOT NULL,
                    first_seen INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL,
                    PRIMARY KEY (player_uuid, ip),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS player_ips (
                    player_uuid VARCHAR(36) NOT NULL,
                    ip BINARY(16) NOT NULL,
                    first_seen BIGINT NOT NULL,
                    last_seen BIGINT NOT NULL,
                    PRIMARY KEY (player_uuid, ip),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                );
            """
    );

    private static final Map<String, String> CREATE_USERS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid CHAR(36) UNIQUE,
                    username VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    verified_at BIGINT,
                    discord_id VARCHAR(255) UNIQUE,
                    verification_code VARCHAR(255),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT UNIQUE,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    verified_at INTEGER,
                    discord_id TEXT UNIQUE,
                    verification_code TEXT,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) UNIQUE,
                    username VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    verified_at BIGINT,
                    discord_id VARCHAR(255) UNIQUE,
                    verification_code VARCHAR(255),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                );
            """
    );

    private static final Map<String, String> CREATE_SESSIONS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_token VARCHAR(255) NOT NULL UNIQUE,
                    user_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_token TEXT NOT NULL UNIQUE,
                    user_id INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_token VARCHAR(255) NOT NULL UNIQUE,
                    user_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """
    );

    private static final Map<String, String> CREATE_RATE_LIMITS_TABLE = Map.of(
            "mysql", """
            CREATE TABLE IF NOT EXISTS rate_limits (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                identifier VARCHAR(255) NOT NULL,
                attempt_type VARCHAR(50) NOT NULL,
                attempt_count INT NOT NULL DEFAULT 0,
                first_attempt BIGINT NOT NULL,
                last_attempt BIGINT NOT NULL,
                locked_until BIGINT,
                UNIQUE KEY idx_identifier_type (identifier, attempt_type),
                INDEX idx_locked (locked_until)
            ) ENGINE=InnoDB;
        """,
            "sqlite", """
            CREATE TABLE IF NOT EXISTS rate_limits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                identifier TEXT NOT NULL,
                attempt_type TEXT NOT NULL,
                attempt_count INTEGER NOT NULL DEFAULT 0,
                first_attempt INTEGER NOT NULL,
                last_attempt INTEGER NOT NULL,
                locked_until INTEGER
            );
            CREATE UNIQUE INDEX IF NOT EXISTS idx_identifier_type
                ON rate_limits(identifier, attempt_type);
            CREATE INDEX IF NOT EXISTS idx_locked
                ON rate_limits(locked_until);
        """,
            "h2", """
            CREATE TABLE IF NOT EXISTS rate_limits (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                identifier VARCHAR(255) NOT NULL,
                attempt_type VARCHAR(50) NOT NULL,
                attempt_count INT NOT NULL DEFAULT 0,
                first_attempt BIGINT NOT NULL,
                last_attempt BIGINT NOT NULL,
                locked_until BIGINT
            );
            CREATE UNIQUE INDEX IF NOT EXISTS idx_identifier_type
                ON rate_limits(identifier, attempt_type);
            CREATE INDEX IF NOT EXISTS idx_locked
                ON rate_limits(locked_until);
        """
    );

    private static final Map<String, String> CREATE_ACTORS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS actors (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    player_uuid CHAR(36),
                    name VARCHAR(255),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS actors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    player_uuid TEXT,
                    name TEXT,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS actors (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    player_uuid VARCHAR(36),
                    name VARCHAR(255),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                );
            """
    );

    private static final Map<String, String> CREATE_PUNISHMENTS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS punishments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    target_uuid CHAR(36),
                    ip BINARY(16),
                    issuer_actor_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    reason TEXT NOT NULL,
                    duration BIGINT,
                    expires_at BIGINT,
                    server_scope VARCHAR(255),
                    server_origin VARCHAR(255) NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    ip_scope BOOLEAN NOT NULL DEFAULT FALSE,
                    FOREIGN KEY (target_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (issuer_actor_id) REFERENCES actors(id) ON DELETE RESTRICT,
                    INDEX idx_target_status (target_uuid, status),
                    INDEX idx_ip_status (ip, status),
                    INDEX idx_type_status (type, status)
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT NOT NULL,
                    target_uuid TEXT,
                    ip BLOB,
                    issuer_actor_id INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    duration INTEGER,
                    expires_at INTEGER,
                    server_scope TEXT,
                    server_origin TEXT NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    ip_scope BOOLEAN NOT NULL DEFAULT FALSE,
                    FOREIGN KEY (target_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (issuer_actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
                CREATE INDEX IF NOT EXISTS idx_target_status ON punishments(target_uuid, status);
                CREATE INDEX IF NOT EXISTS idx_ip_status ON punishments(ip, status);
                CREATE INDEX IF NOT EXISTS idx_type_status ON punishments(type, status);
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS punishments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(20) NOT NULL,
                    target_uuid VARCHAR(36),
                    ip BINARY(16),
                    issuer_actor_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    reason TEXT NOT NULL,
                    duration BIGINT,
                    expires_at BIGINT,
                    server_scope VARCHAR(255),
                    server_origin VARCHAR(255) NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    ip_scope BOOLEAN NOT NULL DEFAULT FALSE,
                    FOREIGN KEY (target_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (issuer_actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
                CREATE INDEX IF NOT EXISTS idx_target_status ON punishments(target_uuid, status);
                CREATE INDEX IF NOT EXISTS idx_ip_status ON punishments(ip, status);
                CREATE INDEX IF NOT EXISTS idx_type_status ON punishments(type, status);
            """
    );

    private static final Map<String, String> CREATE_PUNISHMENT_ACTIONS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS punishment_actions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    punishment_id BIGINT NOT NULL,
                    actor_id INT NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    reason TEXT,
                    action_time BIGINT NOT NULL,
                    FOREIGN KEY (punishment_id) REFERENCES punishments(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS punishment_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    punishment_id INTEGER NOT NULL,
                    actor_id INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    reason TEXT,
                    action_time INTEGER NOT NULL,
                    FOREIGN KEY (punishment_id) REFERENCES punishments(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS punishment_actions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    punishment_id BIGINT NOT NULL,
                    actor_id INT NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    reason TEXT,
                    action_time BIGINT NOT NULL,
                    FOREIGN KEY (punishment_id) REFERENCES punishments(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
            """
    );

    private static final Map<String, String> CREATE_SERVER_LOCKS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS server_locks (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    server_name VARCHAR(255) NOT NULL,
                    issuer_actor_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    reason TEXT NOT NULL,
                    duration BIGINT,
                    server_origin VARCHAR(255) NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
                    FOREIGN KEY (issuer_actor_id) REFERENCES actors(id) ON DELETE RESTRICT,
                    INDEX idx_server_status (server_name, status)
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS server_locks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    server_name TEXT NOT NULL,
                    issuer_actor_id INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    duration INTEGER,
                    server_origin TEXT NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status TEXT NOT NULL DEFAULT 'LOCKED'
                );
                CREATE INDEX IF NOT EXISTS idx_server_status ON server_locks(server_name, status);
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS server_locks (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    server_name VARCHAR(255) NOT NULL,
                    issuer_actor_id INT NOT NULL,
                    created_at BIGINT NOT NULL,
                    reason TEXT NOT NULL,
                    duration BIGINT,
                    server_origin VARCHAR(255) NOT NULL,
                    silent BOOLEAN NOT NULL DEFAULT FALSE,
                    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED'
                );
                CREATE INDEX IF NOT EXISTS idx_server_status ON server_locks(server_name, status);
            """
    );

    private static final Map<String, String> CREATE_SERVER_LOCK_ACTIONS_TABLE = Map.of(
            "mysql", """
                CREATE TABLE IF NOT EXISTS server_lock_actions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    lock_id BIGINT NOT NULL,
                    actor_id INT NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    reason TEXT,
                    action_time BIGINT NOT NULL,
                    FOREIGN KEY (lock_id) REFERENCES server_locks(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                ) ENGINE=InnoDB;
            """,
            "sqlite", """
                CREATE TABLE IF NOT EXISTS server_lock_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lock_id INTEGER NOT NULL,
                    actor_id INTEGER NOT NULL,
                    action TEXT NOT NULL,
                    reason TEXT,
                    action_time INTEGER NOT NULL,
                    FOREIGN KEY (lock_id) REFERENCES server_locks(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
            """,
            "h2", """
                CREATE TABLE IF NOT EXISTS server_lock_actions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    lock_id BIGINT NOT NULL,
                    actor_id INT NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    reason TEXT,
                    action_time BIGINT NOT NULL,
                    FOREIGN KEY (lock_id) REFERENCES server_locks(id) ON DELETE CASCADE,
                    FOREIGN KEY (actor_id) REFERENCES actors(id) ON DELETE RESTRICT
                );
            """
    );

    public static String getCreateTableQuery(String dbType, String tableName) {
        return switch (tableName.toLowerCase()) {
            case "players" -> CREATE_PLAYERS_TABLE.get(dbType);
            case "player_names" -> CREATE_PLAYER_NAMES_TABLE.get(dbType);
            case "player_ips" -> CREATE_PLAYER_IPS_TABLE.get(dbType);
            case "users" -> CREATE_USERS_TABLE.get(dbType);
            case "sessions" -> CREATE_SESSIONS_TABLE.get(dbType);
            case "rate_limits" -> CREATE_RATE_LIMITS_TABLE.get(dbType);
            case "actors" -> CREATE_ACTORS_TABLE.get(dbType);
            case "punishments" -> CREATE_PUNISHMENTS_TABLE.get(dbType);
            case "punishment_actions" -> CREATE_PUNISHMENT_ACTIONS_TABLE.get(dbType);
            case "server_locks" -> CREATE_SERVER_LOCKS_TABLE.get(dbType);
            case "server_lock_actions" -> CREATE_SERVER_LOCK_ACTIONS_TABLE.get(dbType);
            default -> throw new IllegalArgumentException("Unknown table: " + tableName);
        };
    }
}