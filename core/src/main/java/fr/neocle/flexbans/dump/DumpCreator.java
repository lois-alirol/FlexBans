package fr.neocle.flexbans.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public abstract class DumpCreator {

    public Path createDump(String baseFolderPath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("versionInfo", getVersionInfo());
        data.put("cpuCount", Runtime.getRuntime().availableProcessors());
        data.put("cpuName", System.getenv("PROCESSOR_IDENTIFIER"));
        data.put("systemLocale", Locale.getDefault().toString());
        data.put("systemEncoding", Charset.defaultCharset().displayName());
        data.put("ramInfo", getRamInfo());
        data.put("flagsInfo", getFlagsInfo());
        data.put("config", getConfigFields());

        data.put("platformInfo", getPlatformInfo());

        Map<String, Object> additionalData = getAdditionalData();
        if (additionalData != null && !additionalData.isEmpty()) {
            data.putAll(additionalData);
        }

        Path dumpFolder = Paths.get(baseFolderPath, "FlexBans");
        Files.createDirectories(dumpFolder);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        Path dumpFile = dumpFolder.resolve("dump-" + timestamp + ".json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(dumpFile.toFile())) {
            gson.toJson(data, writer);
        }

        return dumpFile;
    }

    protected Map<String, Object> getVersionInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("plugin", "FlexBans");
        versionInfo.put("version", getPluginVersion());
        versionInfo.put("javaName", System.getProperty("java.vm.name"));
        versionInfo.put("javaVendor", System.getProperty("java.vendor"));
        versionInfo.put("javaVersion", System.getProperty("java.version"));
        versionInfo.put("architecture", System.getProperty("os.arch"));
        versionInfo.put("operatingSystem", os.getName());
        versionInfo.put("operatingSystemVersion", os.getVersion());
        return versionInfo;
    }

    protected Map<String, Object> getRamInfo() {
        Map<String, Object> ramInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        ramInfo.put("free", runtime.freeMemory() / 1024 / 1024);
        ramInfo.put("total", runtime.totalMemory() / 1024 / 1024);
        ramInfo.put("max", runtime.maxMemory() / 1024 / 1024);
        return ramInfo;
    }

    protected Map<String, Object> getFlagsInfo() {
        Map<String, Object> flagsInfo = new HashMap<>();
        flagsInfo.put("flags", ManagementFactory.getRuntimeMXBean().getInputArguments());
        return flagsInfo;
    }

    protected Map<String, Object> getConfigFields() {
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

    protected abstract String getPluginVersion();

    protected abstract Map<String, Object> getPlatformInfo();

    protected Map<String, Object> getAdditionalData() {
        return null;
    }
}