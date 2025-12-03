package fr.neocle.flexbans.api.event.bukkit.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerUnwhitelistedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerName;

    public PlayerUnwhitelistedEvent(String playerName) {
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
