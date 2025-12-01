package fr.neocle.flexbans.velocity.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.dump.DumpCreator;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dump extends DumpCreator implements SimpleCommand {
    private final ProxyServer proxyServer;
    private final String pluginVersion;

    public Dump(ProxyServer proxyServer, String pluginVersion) {
        this.proxyServer = proxyServer;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        try {
            Path dumpFile = createDump("plugins");
            source.sendMessage(Component.text("Dump created successfully at: " + dumpFile.toString()));
        } catch (IOException e) {
            source.sendMessage(Component.text("Error creating dump: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    protected String getPluginVersion() {
        return pluginVersion;
    }

    @Override
    protected Map<String, Object> getPlatformInfo() {
        Map<String, Object> platformInfo = new HashMap<>();
        platformInfo.put("platformName", proxyServer.getVersion().getName());
        platformInfo.put("platformVersion", proxyServer.getVersion().getVersion());
        platformInfo.put("onlineMode", proxyServer.getConfiguration().isOnlineMode());
        platformInfo.put("serverIP", proxyServer.getBoundAddress().getHostString());
        platformInfo.put("serverPort", proxyServer.getBoundAddress().getPort());

        List<Map<String, Object>> plugins = new ArrayList<>();
        for (PluginContainer plugin : proxyServer.getPluginManager().getPlugins()) {
            Map<String, Object> pluginInfo = new HashMap<>();
            pluginInfo.put("enabled", plugin.getInstance().isPresent());
            pluginInfo.put("name", plugin.getDescription().getName().orElse("Unknown"));
            pluginInfo.put("version", plugin.getDescription().getVersion().orElse("Unknown"));
            pluginInfo.put("main", plugin.getInstance().map((pl) -> pl.getClass().getName()).orElse("Unknown main class"));
            pluginInfo.put("authors", plugin.getDescription().getAuthors());
            plugins.add(pluginInfo);
        }
        platformInfo.put("plugins", plugins);
        return platformInfo;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.dump");
    }
}