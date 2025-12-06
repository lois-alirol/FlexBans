package fr.neocle.flexbans.command.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.api.whitelist.DiscordWhitelist;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.handler.web.security.oauth.DiscordOAuthHandler;

public abstract class DiscordWhitelistCommand {
    private final DiscordWhitelist whitelist;
    private final DiscordOAuthHandler discordOAuthHandler;

    public DiscordWhitelistCommand(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        this.whitelist = api.getDiscordWhitelist();
        this.discordOAuthHandler = discordOAuthHandler;
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
                ConfigManager.reload();
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.success" : "commands.discord-whitelist.remove-user.success");
            } else {
                sendMessage(source, isAdding ? "commands.discord-whitelist.add-user.already-added" : "commands.discord-whitelist.add-user.not-added");
            }
        }
    }

    protected abstract void sendMessage(Object source, String message);
}
