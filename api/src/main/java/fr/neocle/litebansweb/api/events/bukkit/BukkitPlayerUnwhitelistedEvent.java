package fr.neocle.litebansweb.api.events.bukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitPlayerUnwhitelistedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerName;

    public BukkitPlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
