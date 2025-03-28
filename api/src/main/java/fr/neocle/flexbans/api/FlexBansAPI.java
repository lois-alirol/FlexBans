package fr.neocle.flexbans.api;

import java.util.List;

import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.api.whitelist.DiscordWhitelist;
import fr.neocle.flexbans.api.whitelist.PlayersWhitelist;

public interface FlexBansAPI {

    /**
     * Gets the singleton instance of the FlexBans API.
     *
     * @return The instance of FlexBansAPI.
     */
    static FlexBansAPI getInstance() {
        return FlexBansAPIImpl.getSingletonInstance();
    }

    /**
     * Retrieves the players whitelist.
     *
     * @return The PlayersWhitelist instance.
     */
    PlayersWhitelist getPlayersWhitelist();

    /**
     * Retrieves the Discord whitelist.
     *
     * @return The DiscordWhitelist instance.
     */
    DiscordWhitelist getDiscordWhitelist();
    
    /**
     * Adds a player to the whitelist.
     * @param username name of the player
     * @return true if added successfully, false otherwise
     */
    boolean addPlayerToWhitelist(String username);

    /**
     * Removes a player from the whitelist.
     * @param username name of the player
     * @return true if removed successfully, false otherwise
     */
    boolean removePlayerFromWhitelist(String username);

    /**
     * Gets a list of all whitelisted players.
     * @return List of player names
     */
    List<String> getWhitelistedPlayers();

    /**
     * Checks if a player is whitelisted.
     * @param username name of the player
     * @return true if the player is whitelisted, false otherwise
     */
    boolean isPlayerWhitelisted(String username);

    /**
     * Adds a discord user to the whitelist.
     * @param userId ID of the discord user
     * @return true if added successfully, false otherwise
     */
    boolean addUserToWhitelist(String userId);

    /**
     * Removes a discord user from the whitelist.
     * @param userId ID of the discord user
     * @return true if removed successfully, false otherwise
     */
    boolean removeUserFromWhitelist(String userId);

    /**
     * Gets a list of all whitelisted discord users.
     * @return List of discord users IDs
     */
    List<String> getWhitelistedUsers();

    /**
     * Checks if a discord user is whitelisted.
     * @param userId ID of the discord user
     * @return true if the user is whitelisted, false otherwise
     */
    boolean isUserWhitelisted(String userId);
}
