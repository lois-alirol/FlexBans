package fr.neocle.litebansweb.api.events.bungee;

import net.md_5.bungee.api.plugin.Event;

public class BungeePlayerUnwhitelistedEvent extends Event {
    private final String playerName;

    public BungeePlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
