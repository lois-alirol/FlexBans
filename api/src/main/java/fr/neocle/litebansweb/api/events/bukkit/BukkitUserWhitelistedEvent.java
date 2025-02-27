package fr.neocle.litebansweb.api.events.bukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitUserWhitelistedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String userId;

    public BukkitUserWhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
