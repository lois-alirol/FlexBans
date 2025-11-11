package fr.neocle.flexbans.velocity.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.configs.WebhooksConfigManager;
import fr.neocle.flexbans.handlers.IndexHandler;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.JettyReloader;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.ObjectInputFilter;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class Reload implements SimpleCommand {
    private final Path dataFolder;
    private final AuthenticationHandler oauth2Handler;
    private final IndexHandler indexHandler;
    private final JettyReloader jettyReloader;
    private final Logger logger;

    public Reload(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, JettyReloader jettyReloader, Logger logger) {
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
        this.jettyReloader = jettyReloader;
        this.logger = logger;
    }

    @Override
    public void execute(@NonNull Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("flexbans.reload")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
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

                String lang = (String) ConfigManager.getConfigValue("language");
                LanguageManager.reload(lang);

                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.success"));
                logger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
                logger.severe(LanguageManager.getMessageString("commands.logging.reload.fail"));
            }

            jettyReloader.reload();
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.reload.usage"));
        }
    }
}
