package fr.neocle.litebansweb.api.events.velocity;

public class VelocityDiscordUserLoginEvent {
    private final String userId;
    private final String userAgent;
    private final String ipAddress;

    public VelocityDiscordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        this.userId = userId;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientIP() {
        return ipAddress;
    }
}
