package fr.neocle.flexbans.command.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.whitelist.PlayersWhitelist;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.handler.security.AuthenticationHandler;

public abstract class PlayersWhitelistCommand {
    private final PlayersWhitelist whitelist;
    private final AuthenticationHandler authenticationHandler;

    public PlayersWhitelistCommand(FlexBansAPI api, AuthenticationHandler authenticationHandler) {
        this.whitelist = api.getPlayersWhitelist();
        this.authenticationHandler = authenticationHandler;
    }

    /**
     * Executes the command for adding or removing a player.
     *
     * @param source     The command sender
     * @param playerName The player to add/remove
     * @param isAdding   true = add, false = remove
     */
    public void executeCommand(Object source, String playerName, boolean isAdding) {
        synchronized (this) {
            boolean success = isAdding ? whitelist.addPlayer(playerName) : whitelist.removePlayer(playerName);

            if (success) {
                ConfigManager.reload();
                sendMessage(source, isAdding ? "commands.players-whitelist.add-player.success" : "commands.players-whitelist.remove-player.success");
            } else {
                sendMessage(source, isAdding ? "commands.players-whitelist.add-player.already-added" : "commands.players-whitelist.add-player.not-added");
            }
        }
    }

    protected abstract void sendMessage(Object source, String message);
}
