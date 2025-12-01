package fr.neocle.flexbans.api.events.bungee.whitelist;

import net.md_5.bungee.api.plugin.Event;

public class PlayerUnwhitelistedEvent extends Event {
    private final String playerName;

    public PlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
