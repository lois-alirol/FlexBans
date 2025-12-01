package fr.neocle.flexbans.api.events.bukkit.authentication;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LogoutEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String userId;
    private final String userAgent;
    private final String ipAddress;
    private final String playerName;

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

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
