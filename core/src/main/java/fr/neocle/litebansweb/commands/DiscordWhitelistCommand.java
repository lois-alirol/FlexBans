package fr.neocle.litebansweb.commands;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.api.whitelist.DiscordWhitelist;

import java.nio.file.Path;
import java.util.logging.Logger;

public abstract class DiscordWhitelistCommand {
    private final DiscordWhitelist whitelist;

    public DiscordWhitelistCommand(LitebansWebAPI api, Path configFile, Logger logger) {
        this.whitelist = api.getDiscordWhitelist();
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
            boolean success = isAdding ? whitelist.addUser(playerName) : whitelist.removeUser(playerName);

            if (success) {
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.success" : "commands.discord-whitelist.remove-user.success");
            } else {
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.already-added" : "commands.discord-whitelist.add-user.not-added");
            }
        }
    }

    protected abstract void sendMessage(Object source, String message);
}
