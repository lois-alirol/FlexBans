package fr.neocle.litebansweb.bukkit.commands.SubCommands;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.ResourceLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class Reload implements CommandExecutor {
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansweb.reload")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);

            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord-oauth");
            oauth2Handler.updateConfig();

            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.success"));
            logger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
            logger.severe(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }

        return true;
    }
}
