package fr.neocle.flexbans.commands.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.whitelist.DiscordWhitelist;
import fr.neocle.flexbans.handlers.Security.OAuthHandlers.DiscordOAuthHandler;

import java.util.Map;

public abstract class DiscordWhitelistCommand {
    private final DiscordWhitelist whitelist;
    private final DiscordOAuthHandler discordOAuthHandler;
    private final Map<String, Object> config;

    public DiscordWhitelistCommand(FlexBansAPI api, Map<String, Object> config, DiscordOAuthHandler discordOAuthHandler) {
        this.whitelist = api.getDiscordWhitelist();
        this.discordOAuthHandler = discordOAuthHandler;
        this.config = config;
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
            boolean success = isAdding ? whitelist.addUser(playerName) : whitelist.removeUser(playerName);

            if (success) {
                discordOAuthHandler.updateConfig();
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.success" : "commands.discord-whitelist.remove-user.success");
            } else {
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.already-added" : "commands.discord-whitelist.add-user.not-added");
            }
        }
    }

    protected abstract void sendMessage(Object source, String message);
}
