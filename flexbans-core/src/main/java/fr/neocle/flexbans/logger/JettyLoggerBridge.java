package fr.neocle.flexbans.logger;

import fr.neocle.flexbans.config.ConfigManager;
import org.eclipse.jetty.util.log.Logger;

public class JettyLoggerBridge implements Logger {
    private final FlexLogger flexLogger;
    private final String name;

    public JettyLoggerBridge(String name) {
        this.name = name;
        this.flexLogger = FlexLogger.get(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void warn(String msg, Object... args) {
        flexLogger.warn(msg, args);
    }

    @Override
    public void warn(Throwable thrown) {
        flexLogger.error("Jetty warning", thrown);
    }

    @Override
    public void warn(String msg, Throwable thrown) {
        flexLogger.error(msg, thrown);
    }

    @Override
    public void info(String msg, Object... args) {
        flexLogger.info(msg, args);
    }

    @Override
    public void info(Throwable thrown) {
        flexLogger.info("Jetty info: {}", thrown.getMessage());
    }

    @Override
    public void info(String msg, Throwable thrown) {
        flexLogger.info(msg + " - " + thrown.getMessage());
    }

    @Override
    public boolean isDebugEnabled() {
        return ConfigManager.getBoolean("debug-mode");
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        //ignored, config handles this
    }

    @Override
    public void debug(String msg, Object... args) {
        flexLogger.debug(msg, args);
    }

    @Override
    public void debug(String msg, long value) {
        flexLogger.debug(msg.replace("{}", String.valueOf(value)));
    }

    @Override
    public void debug(Throwable thrown) {
        flexLogger.debug("Jetty debug: {}", thrown.getMessage());
    }

    @Override
    public void debug(String msg, Throwable thrown) {
        flexLogger.debug(msg + " - " + thrown.getMessage());
    }

    @Override
    public Logger getLogger(String name) {
        return this;
    }

    @Override
    public void ignore(Throwable ignored) {}
}