package fr.neocle.litebansweb;

import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import fr.neocle.litebansweb.commands.BaseCommandBungee;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import java.nio.file.Paths;
import java.util.Map;

public class LitebansWebBungee extends Plugin {
    private Bootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), getLogger(), "bungee", null);

        int pluginId = 23870;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getPortFromConfig(), "BungeeCord", getProxy().getVersion());
    }

    private int getPortFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Integer.parseInt(String.valueOf(webserverConfig.getOrDefault("port", "8080")));
    }

    private void registerCommands() {
        BaseCommandBungee baseCommand = new BaseCommandBungee(
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", getLogger()),
            getLogger()
        );
    
        getProxy().getPluginManager().registerCommand(this, baseCommand);
    }
}
