package fr.neocle.flexbans.api.events.velocity.whitelist;

public class PlayerUnwhitelistedEvent {
    private final String playerName;

    public PlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
