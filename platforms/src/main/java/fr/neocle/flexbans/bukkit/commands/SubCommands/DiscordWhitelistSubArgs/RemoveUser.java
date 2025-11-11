package fr.neocle.flexbans.bukkit.commands.SubCommands.DiscordWhitelistSubArgs;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RemoveUser implements CommandExecutor {
    private final DiscordWhitelistCommand whitelistMethods;

    public RemoveUser(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        this.whitelistMethods = new DiscordWhitelistCommand(api, discordOAuthHandler) {
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
        if (!sender.hasPermission("flexbans.discord.remove")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.remove-user.usage"));
            return true;
        }

        String userId = args[1];
        whitelistMethods.executeCommand(sender, userId, false);
        return true;
    }
}
