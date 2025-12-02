package fr.neocle.flexbans.common.commands.subcommands;

import fr. neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr.neocle.flexbans.dump.DumpCreator;
import net.kyori.adventure.text. Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class DumpCommand extends DumpCreator implements ICommandExecutor {
    protected final String pluginVersion;

    public DumpCommand(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();

        try {
            Path dumpFile = createDump("plugins");
            source.sendMessage(Component.text("Dump created successfully at: " + dumpFile));
        } catch (IOException e) {
            source.sendMessage(Component.text("Error creating dump: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections. emptyList();
    }

    @Override
    protected String getPluginVersion() {
        return pluginVersion;
    }

    @Override
    protected abstract Map<String, Object> getPlatformInfo();
}