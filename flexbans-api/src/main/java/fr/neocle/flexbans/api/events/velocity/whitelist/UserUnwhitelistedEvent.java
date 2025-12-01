package fr.neocle.flexbans.api.events.velocity.whitelist;

public class UserUnwhitelistedEvent {

    private final String userId;

    public UserUnwhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
    