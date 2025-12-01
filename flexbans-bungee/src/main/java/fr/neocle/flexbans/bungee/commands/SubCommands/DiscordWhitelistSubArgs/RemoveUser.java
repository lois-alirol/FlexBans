package fr.neocle.flexbans.bungee.commands.SubCommands.DiscordWhitelistSubArgs;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class RemoveUser extends Command {
    private final DiscordWhitelistCommand whitelistMethods;

    public RemoveUser(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        super("remove");
        this.whitelistMethods = new DiscordWhitelistCommand(api, discordOAuthHandler) {
            @Override
            protected void sendMessage(Object sender, String message) {
                if (sender instanceof CommandSender commandSender) {
                    commandSender.sendMessage(LanguageManager.getBungeeMessageComponent(commandSender, message));
                }
            }
        };
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("flexbans.discord.remove")) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.discord-whitelist.remove-user.usage"));
            return;
        }

        String userId = args[1];

        whitelistMethods.executeCommand(sender, userId, false);
    }
}
