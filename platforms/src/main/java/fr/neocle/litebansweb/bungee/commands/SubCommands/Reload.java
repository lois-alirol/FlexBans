package fr.neocle.litebansweb.bungee.commands.SubCommands;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.ResourceLoader;
import net.md_5.bungee.api.CommandSender;
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

        if (sender.hasPermission("litebansweb.reload")) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);
            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord_oauth");
            oauth2Handler.updateConfig(oauthConfig);

            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.success"));
            logger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.fail"));
            logger.severe(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }
    }
}
