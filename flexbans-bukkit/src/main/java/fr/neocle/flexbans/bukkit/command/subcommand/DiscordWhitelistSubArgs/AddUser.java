package fr.neocle.flexbans.bukkit.command.subcommand.DiscordWhitelistSubArgs;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.command.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.locale.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AddUser implements CommandExecutor {
    private final DiscordWhitelistCommand whitelistMethods;

    public AddUser() {
        this.whitelistMethods = new DiscordWhitelistCommand() {
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
        if (!sender.hasPermission("flexbans.discord.add")) {
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
