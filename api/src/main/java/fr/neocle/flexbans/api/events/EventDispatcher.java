package fr.neocle.flexbans.api.events;

public interface EventDispatcher {
    void userWhitelistedEvent(String userId);

    void userUnwhitelistedEvent(String userId);

    void playerWhitelistedEvent(String playerName);

    void playerUnwhitelistedEvent(String playerName);

    void playerLoginEvent(String playerName, String userAgent, String ipAddress);

    void playerRegisterEvent(String playerName, String userAgent, String ipAddress);

    void discordUserLoginEvent(String userId, String userAgent, String ipAddress);

    void logoutEvent(String playerName, String userId, String userAgent, String ipAddress);
}
