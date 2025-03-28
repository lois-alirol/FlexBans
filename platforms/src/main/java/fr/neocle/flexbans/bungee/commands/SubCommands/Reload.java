package fr.neocle.flexbans.bungee.commands.SubCommands;

import fr.neocle.flexbans.handlers.IndexHandler;
import fr.neocle.flexbans.handlers.Security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.ResourceLoader;
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
        super("reload", "flexbans.reload");
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
        this.logger = logger;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender.hasPermission("flexbans.reload")) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);

            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.success"));
            logger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.fail"));
            logger.severe(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }
    }
}
