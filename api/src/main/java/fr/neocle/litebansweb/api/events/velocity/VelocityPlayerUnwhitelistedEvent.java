package fr.neocle.litebansweb.api.events.velocity;

public class VelocityPlayerUnwhitelistedEvent {
    private final String playerName;

    public VelocityPlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
