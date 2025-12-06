package fr.neocle.flexbans.logger;

import fr.neocle.flexbans.config.ConfigManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class FlexLogger {
    private static Logger logger = Logger.getLogger("X");
    private static boolean debugMode = false;

    private FlexLogger() {}

    public static void init(Logger platformLogger) {
        logger = platformLogger;
        debugMode = ConfigManager.getBoolean("debug-mode");
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void info(String message, Object... args) {
        info(String.format(message, args));
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void warn(String message, Object... args) {
        warn(String.format(message, args));
    }

    public static void error(String message) {
        logger.severe(message);
    }

    public static void error(String message, Object... args) {
        error(String.format(message, args));
    }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void debug(String message) {
        if (debugMode) {
            logger.info("[DEBUG] " + message);
        }
    }

    public static void debug(String message, Object... args) {
        debug(String.format(message, args));
    }

    // -------- Convenience aliases --------
    public static void log(String message) {
        info(message);
    }

    public static void log(String message, Object... args) {
        info(message, args);
    }

    public static void config(String message) {
        logger.config(message);
    }

    public static void fine(String message) {
        logger.fine(message);
    }

    public static void finer(String message) {
        logger.finer(message);
    }

    public static void finest(String message) {
        logger.finest(message);
    }
}
