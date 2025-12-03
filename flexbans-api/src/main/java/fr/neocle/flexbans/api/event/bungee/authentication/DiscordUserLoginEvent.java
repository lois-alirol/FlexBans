package fr.neocle.flexbans.api.event.bungee.authentication;

import net.md_5.bungee.api.plugin.Event;

public class DiscordUserLoginEvent extends Event {
    private final String userId;
    private final String userAgent;
    private final String ipAddress;

    public DiscordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        this.userId = userId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
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
