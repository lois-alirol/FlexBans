package fr.neocle.flexbans.bungee;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.events.EventDispatcher;
import fr.neocle.flexbans.api.events.bungee.BungeeEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bungee.commands.BaseCommandBungee;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Paths;
import java.util.Map;

public class FlexBansBungee extends Plugin implements Listener {
    private Bootstrap bootstrap;
    private BungeeAudiences adventure;

    public BungeeAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        this.adventure = BungeeAudiences.create(this);

        EventDispatcher eventDispatcher = new BungeeEventDispatcher(this);

        FlexBansAPIImpl.initialize(
                Paths.get("plugins", "FlexBans", "config.yml"),
                getLogger(),
                eventDispatcher
        );

        getProxy().getPluginManager().registerListener(this, this);

        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), getLogger(), "bungee", eventDispatcher, this);

        int pluginId = 23870;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getAddressFromConfig(), getPortFromConfig(), "BungeeCord", getProxy().getVersion(), true);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
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
                bootstrap.getDiscordOAuthHandler(),
                bootstrap.getIndexHandler(),
                bootstrap.getDatabaseUtils(),
                getLogger(),
                bootstrap.getConfig()
        );

        getProxy().getPluginManager().registerCommand(this, baseCommand);
    }
}
