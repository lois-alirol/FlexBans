package fr.neocle.litebansweb.api.events.bungee;

import net.md_5.bungee.api.plugin.Event;

public class BungeeUserWhitelistedEvent extends Event {
    private final String userId;

    public BungeeUserWhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
