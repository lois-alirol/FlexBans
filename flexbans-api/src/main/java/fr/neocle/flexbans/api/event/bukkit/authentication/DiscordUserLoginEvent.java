package fr.neocle.flexbans.api.event.bukkit.authentication;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DiscordUserLoginEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
