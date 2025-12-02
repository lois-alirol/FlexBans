package fr.neocle.flexbans.common.commands.subcommands;

import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.handlers.IndexHandler;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.JettyReloader;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ReloadCommand implements ICommandExecutor {
    private final Path dataFolder;
    private final AuthenticationHandler oauth2Handler;
    private final IndexHandler indexHandler;
    private final JettyReloader jettyReloader;
    private final Logger logger;

    public ReloadCommand(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler,
                         JettyReloader jettyReloader, Logger logger) {
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
        this.jettyReloader = jettyReloader;
        this.logger = logger;
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
                indexHandler.updateConfig(newConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord-oauth");

                if (oauthConfig != null) {
                    oauth2Handler.updateConfig();
                }

                ConfigManager.reload();
                WebhooksConfigManager.reload();

                String lang = ConfigManager.getString("language");
                LanguageManager.reload(lang);

                source.sendMessage(LanguageManager.getMessageComponent("commands.reload. success"));
                logger.info(LanguageManager.getMessageString("commands.logging.reload. success"));
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
                logger.severe(LanguageManager.getMessageString("commands.logging.reload. fail"));
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