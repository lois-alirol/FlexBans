package fr.neocle.litebansweb.velocity.listener;

import java.util.logging.Logger;
import com.velocitypowered.api.event.Subscribe;

import fr.neocle.litebansweb.api.events.velocity.VelocityDiscordUserLoginEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityLogoutEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerLoginEvent;
import fr.neocle.litebansweb.api.events.velocity.VelocityPlayerRegisterEvent;

public class DashboardEvents {
    private final Logger logger;

    public DashboardEvents(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerLogin(VelocityPlayerLoginEvent event) {
        logger.info("Player " + event.getPlayerName() + " logged in with IP " + event.getClientIP() + " and user agent " + event.getUserAgent());
    }

    @Subscribe
    public void onPlayerRegister(VelocityPlayerRegisterEvent event) {
        logger.info("Player " + event.getPlayerName() + " registered with IP " + event.getClientIP() + " and user agent " + event.getUserAgent());
    }

    @Subscribe
    public void onDiscordUserLogin(VelocityDiscordUserLoginEvent event) {
        logger.info("User " + event.getUserId() + " logged in with IP " + event.getClientIP() + " and user agent " + event.getUserAgent());
    }

    @Subscribe
    public void onUserLogout(VelocityLogoutEvent event) {
        logger.info("User logged in with Discord ID:" + event.getUserId() + " and player name: " + event.getPlayerName() + " logged out with IP " + event.getClientIP() + " and user agent " + event.getUserAgent());
    }
}
