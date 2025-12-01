package fr.neocle.flexbans.api.events.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.velocity.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.LogoutEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.BanAddedEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.KickAddedEvent;
import fr.neocle.flexbans.api.events.velocity.punishments.MuteAddedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserWhitelistedEvent;

import java.util.UUID;

public class VelocityEventDispatcher implements EventDispatcher {
    private final ProxyServer proxyServer;

    public VelocityEventDispatcher(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        UserWhitelistedEvent event = new UserWhitelistedEvent(userId);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        UserUnwhitelistedEvent event = new UserUnwhitelistedEvent(userId);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerWhitelistedEvent(String username) {
        PlayerWhitelistedEvent event = new PlayerWhitelistedEvent(username);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerUnwhitelistedEvent(String username) {
        PlayerUnwhitelistedEvent event = new PlayerUnwhitelistedEvent(username);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        PlayerLoginEvent event = new PlayerLoginEvent(playerName, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        PlayerRegisterEvent event = new PlayerRegisterEvent(playerName, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        DiscordUserLoginEvent event = new DiscordUserLoginEvent(userId, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        LogoutEvent event = new LogoutEvent(playerName, userId, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void banAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                               String reason, long duration, String serverScope,
                               String serverOrigin, boolean silent, boolean ipScope) {

        BanAddedEvent event = new BanAddedEvent(
                targetUUID, targetName, senderUUID, senderName,
                reason, duration, serverScope, serverOrigin, silent, ipScope
        );
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void muteAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                                String reason, long duration, String serverScope,
                                String serverOrigin, boolean silent, boolean ipScope) {

        MuteAddedEvent event = new MuteAddedEvent(
                targetUUID, targetName, senderUUID, senderName,
                reason, duration, serverScope, serverOrigin, silent, ipScope
        );
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void kickAddedEvent(UUID targetUUID, String targetName, UUID senderUUID, String senderName,
                                String reason, String serverOrigin,
                                boolean silent, boolean ipScope) {

        KickAddedEvent event = new KickAddedEvent(
                targetUUID, targetName, senderUUID, senderName,
                reason, serverOrigin, silent, ipScope
        );
        proxyServer.getEventManager().fire(event);
    }
}
