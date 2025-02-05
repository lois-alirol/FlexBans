package fr.neocle.litebansweb.commands.SubCommands.Bungee;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.utils.ResourceLoader;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class Reload extends Command {
    private final Path dataFolder;
    private final AuthenticationHandler oauth2Handler;
    private final IndexHandler indexHandler;
    private final Logger logger;

    public Reload(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, Logger logger) {
        super("reload", "litebansweb.reload");
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
        this.logger = logger;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);
            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord_oauth");
            oauth2Handler.updateConfig(oauthConfig);

            sender.sendMessage(new TextComponent("LiteBansWeb configuration reloaded successfully."));
            logger.info("LiteBansWeb configuration reloaded successfully.");
        } else {
            sender.sendMessage(new TextComponent("Failed to reload configuration."));
            logger.severe("Failed to reload configuration.");
        }
    }
}
