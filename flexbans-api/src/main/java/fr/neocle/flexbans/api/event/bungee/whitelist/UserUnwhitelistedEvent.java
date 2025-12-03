package fr.neocle.flexbans.api.event.bungee.whitelist;

import net.md_5.bungee.api.plugin.Event;

public class UserUnwhitelistedEvent extends Event {
    private final String userId;

    public UserUnwhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
