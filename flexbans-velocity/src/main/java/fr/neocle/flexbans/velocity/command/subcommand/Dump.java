package fr.neocle.flexbans.velocity.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.subcommand.DumpCommand;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Dump extends DumpCommand {
    private final ProxyServer proxyServer;

    public Dump(ProxyServer proxyServer, String pluginVersion) {
        super(pluginVersion);
        this.proxyServer = proxyServer;
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("dump")
                .requires(source -> source.hasPermission("flexbans.dump"))
                .executes(ctx -> {
                    this.execute(new VelocityCommandInvocation(ctx));
                    return Command.SINGLE_SUCCESS;
                });
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
            pluginInfo.put("main", plugin.getInstance().map(pl -> pl.getClass().getName()).orElse("Unknown main class"));
            pluginInfo.put("authors", plugin.getDescription().getAuthors());
            plugins.add(pluginInfo);
        }
        platformInfo.put("plugins", plugins);
        return platformInfo;
    }
}