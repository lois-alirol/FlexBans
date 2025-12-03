package fr.neocle.flexbans.api.event.velocity.whitelist;

public class UserWhitelistedEvent {
    private final String userId;

    public UserWhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
