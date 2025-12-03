package fr.neocle.flexbans.api.event.velocity.authentication;

public class PlayerLoginEvent {
    private final String playerName;
    private final String userAgent;
    private final String ipAddress;

    public PlayerLoginEvent(String playerName, String userAgent, String ipAddress) {
        this.playerName = playerName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getClientIP() {
        return ipAddress;
    }
}
