package fr.neocle.flexbans.logger;

import fr.neocle.flexbans.config.ConfigManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public final class FlexLogger {

    private static final Map<String, FlexLogger> LOGGER_CACHE = new ConcurrentHashMap<>();
    private static Logger platformLogger;
    private static boolean debugMode = false;

    private final Logger contextLogger;
    private final String contextName;

    private FlexLogger(Logger logger, String contextName) {
        this.contextLogger = logger;
        this.contextName = contextName;
    }

    public static void init(Logger rootLogger) {
        platformLogger = rootLogger;
        debugMode = ConfigManager.getBoolean("debug-mode");
    }

    public static FlexLogger get(Class<?> clazz) {
        return LOGGER_CACHE.computeIfAbsent(clazz.getSimpleName(), name -> {
            Logger base = (platformLogger != null)
                    ? platformLogger
                    : Logger.getLogger("FlexBans");
            return new FlexLogger(base, clazz.getSimpleName());
        });
    }

    public static FlexLogger get(String name) {
        return LOGGER_CACHE.computeIfAbsent(name, n -> {
            Logger base = (platformLogger != null)
                    ? platformLogger
                    : Logger.getLogger("FlexBans");
            return new FlexLogger(base, n);
        });
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args);
    }

    public void error(String message, Object... args) {
        if (args != null && args.length > 0) {
            Object last = args[args.length - 1];

            if (last instanceof Throwable throwable) {
                Object[] trimmed = new Object[args.length - 1];
                System.arraycopy(args, 0, trimmed, 0, args.length - 1);

                String formatted = format(message, trimmed);
                contextLogger.log(Level.SEVERE, "[" + contextName + "] " + formatted, throwable);
                return;
            }
        }

        String formatted = format(message, args);
        contextLogger.log(Level.SEVERE, "[" + contextName + "] " + formatted);
    }

    public void error(String message, Throwable throwable) {
        contextLogger.log(Level.SEVERE, "[" + contextName + "] " + message, throwable);
    }

    public void debug(String message, Object... args) {
        if (debugMode) {
            log(Level.INFO, "[DEBUG] " + message, args);
        }
    }

    private void log(Level level, String message, Object... args) {
        if (!contextLogger.isLoggable(level)) return;

        String formatted = format(message, args);
        contextLogger.log(level, "[" + contextName + "] " + formatted);
    }

    private String format(String message, Object... args) {
        if (args == null) return message;

        for (Object arg : args) {
            message = message.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(arg)));
        }
        return message;
    }
}