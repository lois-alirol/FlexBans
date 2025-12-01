package fr.neocle.flexbans.api.events.bukkit.whitelist;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerWhitelistedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String playerName;

    public PlayerWhitelistedEvent(String playerName) {
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
