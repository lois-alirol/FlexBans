package fr.neocle.litebansweb.utils;

import org.eclipse.jetty.server.Server;

import java.util.logging.Logger;

public class JettyReloader {
    private final Server server;
    private final Logger logger;

    public JettyReloader(Server server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void reload() {
        try {
            logger.info("Stopping webserver...");
            server.stop();
            logger.info("Starting webserver...");
            server.start();
        } catch (Exception e) {
            logger.info("Couldn't restart webserver!");
            e.printStackTrace();
        }
    }
}
