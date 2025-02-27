package fr.neocle.litebansweb.bukkit.commands.SubCommands.DiscordWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.DiscordWhitelistCommand;
import fr.neocle.litebansweb.locale.LanguageManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.logging.Logger;

public class AddUser implements CommandExecutor {
    private final DiscordWhitelistCommand whitelistMethods;

    public AddUser(LitebansWebAPI api, Path dataFolder, Logger logger) {
        this.whitelistMethods = new DiscordWhitelistCommand(api, dataFolder.resolve("config.yml"), logger) {
            @Override
            protected void sendMessage(Object sender, String message) {
                if (sender instanceof CommandSender commandSender) {
                    commandSender.sendMessage(LanguageManager.getMessageComponent(message));
                }
            }
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansweb.discord.add")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.add-user.usage"));
            return true;
        }

        String userId = args[1];
        whitelistMethods.executeCommand(sender, userId, true);
        return true;
    }
}
