package fr.neocle.flexbans.api.events.bungee.whitelist;

import net.md_5.bungee.api.plugin.Event;

public class UserWhitelistedEvent extends Event {
    private final String userId;

    public UserWhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
