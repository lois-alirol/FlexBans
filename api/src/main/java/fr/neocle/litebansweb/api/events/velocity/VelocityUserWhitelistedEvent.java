package fr.neocle.litebansweb.api.events.velocity;

public class VelocityUserWhitelistedEvent {
    private final String userId;

    public VelocityUserWhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
