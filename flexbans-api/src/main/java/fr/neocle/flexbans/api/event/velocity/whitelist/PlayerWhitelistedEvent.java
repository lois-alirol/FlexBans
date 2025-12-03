package fr.neocle.flexbans.api.event.velocity.whitelist;

public class PlayerWhitelistedEvent {
    private final String playerName;

    public PlayerWhitelistedEvent(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
