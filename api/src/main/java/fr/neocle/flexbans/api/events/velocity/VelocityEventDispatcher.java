package fr.neocle.flexbans.api.events.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.velocity.authentication.DiscordUserLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.LogoutEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerLoginEvent;
import fr.neocle.flexbans.api.events.velocity.authentication.PlayerRegisterEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.PlayerWhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserUnwhitelistedEvent;
import fr.neocle.flexbans.api.events.velocity.whitelist.UserWhitelistedEvent;

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
}
