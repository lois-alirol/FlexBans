package fr.neocle.litebansweb.api.events.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.litebansweb.api.events.EventDispatcher;

public class VelocityEventDispatcher implements EventDispatcher {
    private final ProxyServer proxyServer;

    public VelocityEventDispatcher(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void userWhitelistedEvent(String userId) {
        VelocityUserWhitelistedEvent event = new VelocityUserWhitelistedEvent(userId);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void userUnwhitelistedEvent(String userId) {
        VelocityUserUnwhitelistedEvent event = new VelocityUserUnwhitelistedEvent(userId);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerWhitelistedEvent(String username) {
        VelocityPlayerWhitelistedEvent event = new VelocityPlayerWhitelistedEvent(username);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerUnwhitelistedEvent(String username) {
        VelocityPlayerUnwhitelistedEvent event = new VelocityPlayerUnwhitelistedEvent(username);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerLoginEvent(String playerName, String userAgent, String ipAddress) {
        VelocityPlayerLoginEvent event = new VelocityPlayerLoginEvent(playerName, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void playerRegisterEvent(String playerName, String userAgent, String ipAddress) {
        VelocityPlayerRegisterEvent event = new VelocityPlayerRegisterEvent(playerName, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void discordUserLoginEvent(String userId, String userAgent, String ipAddress) {
        VelocityDiscordUserLoginEvent event = new VelocityDiscordUserLoginEvent(userId, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }

    @Override
    public void logoutEvent(String playerName, String userId, String userAgent, String ipAddress) {
        VelocityLogoutEvent event = new VelocityLogoutEvent(playerName, userId, userAgent, ipAddress);
        proxyServer.getEventManager().fire(event);
    }
}
