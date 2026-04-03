package fr.neocle.flexbans.util;

import fr.neocle.flexbans.logger.FlexLogger;
import org.eclipse.jetty.server.Server;

import java.util.logging.Logger;

public class JettyReloader {
    private final Server server;

    public JettyReloader(Server server) {
        this.server = server;
    }

    public void reload() {

    }
}
