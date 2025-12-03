package fr.neocle.flexbans.api.event;

import java.util.UUID;

public interface EventDispatcher {
    void userWhitelistedEvent(String userId);

    void userUnwhitelistedEvent(String userId);

    void playerWhitelistedEvent(String playerName);

    void playerUnwhitelistedEvent(String playerName);

    void playerLoginEvent(String playerName, String userAgent, String ipAddress);

    void playerRegisterEvent(String playerName, String userAgent, String ipAddress);

    void discordUserLoginEvent(String userId, String userAgent, String ipAddress);

    void logoutEvent(String playerName, String userId, String userAgent, String ipAddress);

    void banAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                       String reason, long duration, String serverScope,
                       String serverOrigin, boolean silent, boolean ipScope);

    void muteAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                        String reason, long duration, String serverScope,
                        String serverOrigin, boolean silent, boolean ipScope);

    void kickAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                        String reason, String serverOrigin,
                        boolean silent, boolean ipScope);
}
