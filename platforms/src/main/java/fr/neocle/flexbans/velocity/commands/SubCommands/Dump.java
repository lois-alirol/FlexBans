package fr.neocle.flexbans.velocity.commands.SubCommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.plugin.PluginContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Dump implements SimpleCommand {
    private final ProxyServer proxyServer;

    public Dump(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("versionInfo", getVersionInfo());
            data.put("cpuCount", Runtime.getRuntime().availableProcessors());
            data.put("cpuName", System.getenv("PROCESSOR_IDENTIFIER"));
            data.put("systemLocale", Locale.getDefault().toString());
            data.put("systemEncoding", Charset.defaultCharset().displayName());
            data.put("ramInfo", getRamInfo());
            data.put("platformInfo", getPlatformInfo());
            data.put("flagsInfo", getFlagsInfo());
            data.put("config", getConfigFields());

            Path dumpFolder = Paths.get("plugins", "FlexBans");
            Files.createDirectories(dumpFolder);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            Path dumpFile = dumpFolder.resolve("dump-" + timestamp + ".json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(dumpFile.toFile())) {
                gson.toJson(data, writer);
            }

            source.sendMessage(Component.text("Dump created successfully!"));
        } catch (IOException e) {
            source.sendMessage(Component.text("Error creating dump: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private Map<String, Object> getVersionInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("plugin", "FlexBans");
        versionInfo.put("version", "1.0");
        versionInfo.put("javaName", System.getProperty("java.vm.name"));
        versionInfo.put("javaVendor", System.getProperty("java.vendor"));
        versionInfo.put("javaVersion", System.getProperty("java.version"));
        versionInfo.put("architecture", System.getProperty("os.arch"));
        versionInfo.put("operatingSystem", os.getName());
        versionInfo.put("operatingSystemVersion", os.getVersion());
        return versionInfo;
    }

    private Map<String, Object> getRamInfo() {
        Map<String, Object> ramInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        ramInfo.put("free", runtime.freeMemory() / 1024 / 1024);
        ramInfo.put("total", runtime.totalMemory() / 1024 / 1024);
        ramInfo.put("max", runtime.maxMemory() / 1024 / 1024);
        return ramInfo;
    }

    private Map<String, Object> getPlatformInfo() {
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

    private Map<String, Object> getFlagsInfo() {
        Map<String, Object> flagsInfo = new HashMap<>();
        flagsInfo.put("flags", ManagementFactory.getRuntimeMXBean().getInputArguments());
        return flagsInfo;
    }

    private Map<String, Object> getConfigFields() {
        Yaml yaml = new Yaml();
        Map<String, Object> config = new HashMap<>();
        try (InputStream input = getClass().getResourceAsStream("/config.yml")) {
            if (input != null) {
                config = yaml.load(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.dump");
    }
}
