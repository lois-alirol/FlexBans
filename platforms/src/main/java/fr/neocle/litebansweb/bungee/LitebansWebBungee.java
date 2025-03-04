package fr.neocle.litebansweb.bungee;

import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import fr.neocle.litebansweb.bungee.commands.BaseCommandBungee;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.Bootstrap;
import fr.neocle.litebansweb.api.events.EventDispatcher;
import fr.neocle.litebansweb.api.events.bungee.BungeeEventDispatcher;
import fr.neocle.litebansweb.api.events.bungee.BungeeUserWhitelistedEvent;
import fr.neocle.litebansweb.api.impl.LitebansWebAPIImpl;

import java.nio.file.Paths;
import java.util.Map;

public class LitebansWebBungee extends Plugin implements Listener {
    private Bootstrap bootstrap;
    private BungeeAudiences adventure;

    public BungeeAudiences adventure() {
        if(this.adventure == null) {
          throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
      }

    @Override
    public void onEnable() {
        this.adventure = BungeeAudiences.create(this);

        EventDispatcher eventDispatcher = new BungeeEventDispatcher(this);

        LitebansWebAPIImpl.initialize(
            Paths.get("plugins", "LitebansWeb", "config.yml"), 
            getLogger(), 
            eventDispatcher
        );

        getProxy().getPluginManager().registerListener(this, this);
        
        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), getLogger(), "bungee", null, eventDispatcher);

        int pluginId = 23870;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "BungeeCord", getProxy().getVersion());
    }

    @Override
    public void onDisable() {
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    private String getAddressFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return String.valueOf(webserverConfig.getOrDefault("url", "undefined, check config.yml"));
    }

    private int getPortFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Integer.parseInt(String.valueOf(webserverConfig.getOrDefault("port", "8080")));
    }

    private void registerCommands() {
        BaseCommandBungee baseCommand = new BaseCommandBungee(
            bootstrap.getAPI(),
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", getLogger()),
            getLogger()
        );
    
        getProxy().getPluginManager().registerCommand(this, baseCommand);
    }

    @EventHandler
    public void onPlayerWhitelisted(BungeeUserWhitelistedEvent event) {
        getLogger().info("User " + event.getUserId() + " was whitelisted!");
    }

}
