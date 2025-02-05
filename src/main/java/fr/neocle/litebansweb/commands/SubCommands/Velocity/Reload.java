package fr.neocle.litebansweb.commands.SubCommands.Velocity;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.utils.ResourceLoader;
import net.kyori.adventure.text.Component;

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
            source.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
            Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);
            if (newConfig != null) {
                indexHandler.updateConfig(newConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord_oauth");
                oauth2Handler.updateConfig(oauthConfig);

                source.sendMessage(Component.text("LiteBansWeb configuration reloaded successfully."));
                logger.info("LiteBansWeb configuration reloaded successfully.");
            } else {
                source.sendMessage(Component.text("Failed to reload configuration."));
                logger.severe("Failed to reload configuration.");
            }
        } else {
            source.sendMessage(Component.text("Usage: /litebansweb reload"));
        }
    }
}
