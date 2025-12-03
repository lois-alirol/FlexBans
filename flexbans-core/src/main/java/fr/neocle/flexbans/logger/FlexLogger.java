package fr.neocle.flexbans.logger;

import fr.neocle.flexbans.config.ConfigManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FlexLogger {
    private static Logger logger;

    public static void init(Logger platformLogger) {
        logger = platformLogger;
    }

    public static boolean isDebugMode() {
        return ConfigManager.getBoolean("debug-mode");
    }

    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public static void info(String message, Object... args) {
        info(String.format(message, args));
    }

    public static void warn(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    public static void warn(String message, Object... args) {
        warn(String.format(message, args));
    }

    public static void error(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }

    public static void error(String message, Object... args) {
        error(String.format(message, args));
    }

    public static void error(String message, Throwable throwable) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, throwable);
        }
    }

    public static void debug(String message) {
        if (isDebugMode() && logger != null) {
            logger.info("[DEBUG] " + message);
        }
    }

    public static void debug(String message, Object... args) {
        debug(String.format(message, args));
    }

    public static void log(String message) {
        info(message);
    }

    public static void log(String message, Object... args) {
        info(message, args);
    }

    public static void config(String message) {
        if (logger != null) {
            logger.config(message);
        }
    }

    public static void fine(String message) {
        if (logger != null) {
            logger.fine(message);
        }
    }

    public static void finer(String message) {
        if (logger != null) {
            logger.finer(message);
        }
    }

    public static void finest(String message) {
        if (logger != null) {
            logger.finest(message);
        }
    }
}