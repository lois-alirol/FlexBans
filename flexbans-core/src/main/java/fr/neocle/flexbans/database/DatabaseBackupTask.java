package fr.neocle.flexbans.database;

import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DatabaseBackupTask implements Runnable {
    private final String pluginFolderPath;
    private final String databaseType;

    private static final FlexLogger LOGGER = FlexLogger.get(DatabaseBackupTask.class);

    public DatabaseBackupTask(String pluginFolderPath, String databaseType) {
        this.pluginFolderPath = pluginFolderPath;
        this.databaseType = databaseType;
    }

    public void start() {
        TaskScheduler.get().runRepeating(this, 10800000);
    }

    @Override
    public void run() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupDir = Paths.get(pluginFolderPath, "cache", "backups");
            Files.createDirectories(backupDir);

            switch (databaseType.toLowerCase()) {
                case "sqlite": {
                    Path source = Paths.get(pluginFolderPath, "sqlite.db");
                    if (!Files.exists(source)) {
                        LOGGER.warn("SQLite database file not found, skipping backup.");
                        break;
                    }
                    Path destination = backupDir.resolve("backup_" + timestamp + "_sqlite.db");
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("SQLite database backup created: {}", destination);
                    break;
                }
                case "h2": {
                    Path source = Paths.get(pluginFolderPath, "h2.mv.db");
                    if (!Files.exists(source)) {
                        LOGGER.warn("H2 database file not found, skipping backup.");
                        break;
                    }
                    Path destination = backupDir.resolve("backup_" + timestamp + "_h2.mv.db");
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.debug("H2 database backup created: {}", destination);
                    break;
                }
                case "mysql": {
                    LOGGER.debug("MySQL database backup not implemented yet!");
                    break;
                }
                default:
                    LOGGER.debug("Unsupported database type for backup.");
            }

            Files.list(backupDir)
                    .filter(Files::isRegularFile)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .skip(32)
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                            LOGGER.info("Deleted old backup: {}", file.getFileName());
                        } catch (Exception e) {
                            LOGGER.warn("Failed to delete old backup: {} - ", file.getFileName(), e);
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("Failed to create database backup: ", e);
        }
    }
}
