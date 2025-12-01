package fr.neocle.flexbans.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DatabaseBackupTask implements Runnable {
    private final String pluginFolderPath;
    private final String databaseType;
    private final Logger logger;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DatabaseBackupTask(String pluginFolderPath, String databaseType, Logger logger) {
        this.pluginFolderPath = pluginFolderPath;
        this.databaseType = databaseType;
        this.logger = logger;
    }

    public void startBackupCreationTask() {
        scheduler.scheduleAtFixedRate(this::run, 0, 3, TimeUnit.HOURS);
    }

    @Override
    public void run() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backupDir = Paths.get(pluginFolderPath, "cache", "backups");
            Files.createDirectories(backupDir);

            switch (databaseType.toLowerCase()) {
                case "sqlite":
                case "h2": {
                    String dbFile = databaseType.equals("sqlite") ? "database.db" : "database.db";
                    Path source = Paths.get(pluginFolderPath, dbFile);
                    Path destination = backupDir.resolve("backup_" + timestamp + "_" + dbFile);
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Database backup created: " + destination);
                    break;
                }
                case "mysql": {
                    break;
                }
                default:
                    logger.warning("Unsupported database type for backup.");
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
                            logger.info("Deleted old backup: " + file.getFileName());
                        } catch (Exception e) {
                            logger.warning("Failed to delete old backup: " + file.getFileName() + " - " + e.getMessage());
                        }
                    });

        } catch (Exception e) {
            logger.severe("Failed to create database backup: " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            logger.info("Database backups creation scheduler stopped");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warning("Database backups creation scheduler interrupted while shutting down");
        }
    }
}
