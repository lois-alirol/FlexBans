package fr.neocle.flexbans.util.loader;

import org.eclipse.jetty.server.Server;

public class JettyReloader {
    private final Server server;

    public JettyReloader(Server server) {
        this.server = server;
    }

    public void reload() {

    }
}
