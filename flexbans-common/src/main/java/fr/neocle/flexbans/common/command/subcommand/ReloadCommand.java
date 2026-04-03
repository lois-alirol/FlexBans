package fr.neocle.flexbans.common.command.subcommand;

import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.config.WebhooksConfigManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.loader.JettyReloader;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReloadCommand implements ICommandExecutor {
    private final JettyReloader jettyReloader;

    private static final FlexLogger LOGGER = FlexLogger.get(ReloadCommand.class);

    public ReloadCommand(Path dataFolder,
                         JettyReloader jettyReloader) {
        this.jettyReloader = jettyReloader;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();

        if (!source.hasPermission("flexbans.reload")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        String[] args = invocation.getArguments();
        if (args. length > 0 && args[0].equalsIgnoreCase("reload")) {
            Map<String, Object> newConfig = ConfigManager.getConfig();
            if (newConfig != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord-oauth");

                ConfigManager.reload();
                WebhooksConfigManager.reload();

                String lang = ConfigManager.getString("language");
                LanguageManager.reload(lang);

                source.sendMessage(LanguageManager.getMessageComponent("commands.reload. success"));
                LOGGER.info(LanguageManager.getMessageString("commands.logging.reload. success"));
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
                LOGGER.error(LanguageManager.getMessageString("commands.logging.reload. fail"));
            }

            jettyReloader.reload();
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.reload.usage"));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections. singletonList("reload");
    }
}