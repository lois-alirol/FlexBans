package fr.neocle.litebansweb.commands.SubCommands.Bukkit;

import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.utils.ResourceLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

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
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Map<String, Object> newConfig = ResourceLoader.loadConfig(dataFolder, logger);

        if (newConfig != null) {
            indexHandler.updateConfig(newConfig);

            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord_oauth");
            oauth2Handler.updateConfig(oauthConfig);

            sender.sendMessage(ChatColor.GREEN + "LiteBansWeb configuration reloaded successfully.");
            logger.info("LiteBansWeb configuration reloaded successfully.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to reload configuration.");
            logger.severe("Failed to reload configuration.");
        }

        return true;
    }
}
