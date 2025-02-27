package fr.neocle.litebansweb.api.events.bungee;

import net.md_5.bungee.api.plugin.Event;

public class BungeeUserUnwhitelistedEvent extends Event {
    private final String userId;

    public BungeeUserUnwhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
