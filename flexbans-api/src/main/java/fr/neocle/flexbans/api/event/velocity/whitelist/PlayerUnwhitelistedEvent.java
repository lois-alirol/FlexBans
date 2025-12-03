package fr.neocle.flexbans.api.event.velocity.whitelist;

public class PlayerUnwhitelistedEvent {
    private final String playerName;

    public PlayerUnwhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
