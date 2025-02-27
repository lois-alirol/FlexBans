package fr.neocle.litebansweb.commands;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.api.whitelist.PlayersWhitelist;

import java.nio.file.Path;
import java.util.logging.Logger;

public abstract class PlayersWhitelistCommand {
    private final PlayersWhitelist whitelist;

    public PlayersWhitelistCommand(LitebansWebAPI api, Path configFile, Logger logger) {
        this.whitelist = api.getPlayersWhitelist();
    }

    /**
     * Executes the command for adding or removing a player.
     *
     * @param source The command sender
     * @param playerName The player to add/remove
     * @param isAdding true = add, false = remove
     */
    public void executeCommand(Object source, String playerName, boolean isAdding) {
        synchronized (this) {
            boolean success = isAdding ? whitelist.addPlayer(playerName) : whitelist.removePlayer(playerName);

            if (success) {
                sendMessage(source, isAdding ? "commands.players-whitelist.add-player.success" : "commands.players-whitelist.remove-player.success");
            } else {
                sendMessage(source, isAdding ? "commands.players-whitelist.add-player.already-added" : "commands.players-whitelist.add-player.not-added");
            }
        }
    }

    protected abstract void sendMessage(Object source, String message);
}
