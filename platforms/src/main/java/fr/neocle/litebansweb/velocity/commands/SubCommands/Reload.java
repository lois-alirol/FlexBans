package fr.neocle.litebansweb.velocity.commands.SubCommands;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.ResourceLoader;

public class Reload implements SimpleCommand {
    private final Path dataFolder;
    private final AuthenticationHandler oauth2Handler;
    private final IndexHandler indexHandler;
    private final Logger logger;

    public Reload(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, Logger logger) {
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
        this.logger = logger;
    }

    @Override
    public void execute(@NonNull Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("litebansweb.reload")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
            Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);
            if (newConfig != null) {
                indexHandler.updateConfig(newConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord_oauth");
                oauth2Handler.updateConfig(oauthConfig);

                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.success"));
                logger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
            } else {
                source.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
                logger.severe(LanguageManager.getMessageString("commands.logging.reload.fail"));
            }
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.reload.usage"));
        }
    }
}
