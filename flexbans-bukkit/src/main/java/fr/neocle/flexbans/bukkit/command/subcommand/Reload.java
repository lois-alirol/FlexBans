package fr.neocle.flexbans.bukkit.command.subcommand;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.handler.web.IndexHandler;
import fr.neocle.flexbans.handler.web.security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.Map;

public class Reload implements CommandExecutor {
    private final Path dataFolder;
    private final AuthenticationHandler oauth2Handler;
    private final IndexHandler indexHandler;

    public Reload(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler) {
        this.dataFolder = dataFolder;
        this.oauth2Handler = oauth2Handler;
        this.indexHandler = indexHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("flexbans.reload")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        Map<String, Object> newConfig = ConfigManager.getConfig();

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);

            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord-oauth");
            oauth2Handler.updateConfig();

            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.success"));
            FlexLogger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
            FlexLogger.error(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }

        return true;
    }
}
