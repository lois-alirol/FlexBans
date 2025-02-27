package fr.neocle.litebansweb.api.events.velocity;

public class VelocityPlayerWhitelistedEvent {
    private final String playerName;

    public VelocityPlayerWhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
