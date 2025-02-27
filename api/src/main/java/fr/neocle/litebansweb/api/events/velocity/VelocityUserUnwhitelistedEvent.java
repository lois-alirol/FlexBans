package fr.neocle.litebansweb.api.events.velocity;

public class VelocityUserUnwhitelistedEvent {

    private final String userId;

    public VelocityUserUnwhitelistedEvent(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
    