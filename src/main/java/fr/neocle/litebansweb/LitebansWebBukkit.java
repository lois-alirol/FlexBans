package fr.neocle.litebansweb;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neocle.litebansweb.commands.BaseCommandBukkit;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import java.nio.file.Paths;
import java.util.Map;

public class LitebansWebBukkit extends JavaPlugin {
    private Bootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new Bootstrap();
        bootstrap.initialize(Paths.get("plugins", "LitebansWeb"), getLogger(), "spigot", null);

        int pluginId = 23868;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);

        registerCommands();
        bootstrap.startWebServer(getPortFromConfig());
        bootstrap.logServerStartupInfo(getPortFromConfig(), "Spigot", getServer().getVersion());
    }

    private int getPortFromConfig() {
        @SuppressWarnings("unchecked")
        Map<String, Object> webserverConfig = (Map<String, Object>) bootstrap.getConfig().get("webserver");
        return Integer.parseInt(String.valueOf(webserverConfig.getOrDefault("port", "8080")));
    }

    private void registerCommands() {
        BaseCommandBukkit baseCommand = new BaseCommandBukkit(
            bootstrap.getDataFolder(),
            bootstrap.getAuthenticatorHandler(),
            bootstrap.getIndexHandler(),
            new DatabaseUtils("./plugins/LitebansWeb", getLogger()),
            getLogger()
        );
    
        getCommand("litebansweb").setExecutor(baseCommand);
        getCommand("litebansweb").setTabCompleter(baseCommand);
    }
}
