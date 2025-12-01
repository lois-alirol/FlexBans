package fr.neocle.flexbans.api.events.bungee.authentication;

import net.md_5.bungee.api.plugin.Event;

public class LogoutEvent extends Event {
    private final String playerName;
    private final String userId;
    private final String userAgent;
    private final String ipAddress;

    public LogoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        this.playerName = playerName;
        this.userId = userId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientIP() {
        return ipAddress;
    }
}
