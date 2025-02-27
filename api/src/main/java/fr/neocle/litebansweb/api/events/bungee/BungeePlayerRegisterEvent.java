package fr.neocle.litebansweb.api.events.bungee;

import net.md_5.bungee.api.plugin.Event;

public class BungeePlayerRegisterEvent extends Event {
    private final String playerName;
    private final String userAgent;
    private final String ipAddress;

    public BungeePlayerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        this.playerName = playerName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientIP() {
        return ipAddress;
    }
}
