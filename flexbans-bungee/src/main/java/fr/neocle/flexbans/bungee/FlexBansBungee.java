package fr.neocle.flexbans.bungee;

import fr.neocle.flexbans.Bootstrap;
import fr.neocle.flexbans.api.event.EventDispatcher;
import fr.neocle.flexbans.api.event.bungee.BungeeEventDispatcher;
import fr.neocle.flexbans.api.impl.FlexBansAPIImpl;
import fr.neocle.flexbans.bungee.command.BaseCommandBungee;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Paths;

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
        bootstrap.initialize(Paths.get("plugins", "FlexBans"), getLogger(), "bungee", eventDispatcher, getProxy());

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
        return "";
    }

    private int getPortFromConfig() {
        return 0;
    }

    private void registerCommands() {
        BaseCommandBungee baseCommand = new BaseCommandBungee(
                bootstrap.getAPI(),
                bootstrap.getDataFolder(),
                bootstrap.getAuthenticatorHandler(),
                bootstrap.getDiscordOAuthHandler(),
                bootstrap.getIndexHandler(),
                bootstrap.getDatabaseUtils(),
                getLogger()
        );

        getProxy().getPluginManager().registerCommand(this, baseCommand);
    }
}
