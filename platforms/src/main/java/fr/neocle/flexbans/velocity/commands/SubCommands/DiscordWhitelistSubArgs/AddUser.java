package fr.neocle.flexbans.velocity.commands.SubCommands.DiscordWhitelistSubArgs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;

import java.util.Map;

public class AddUser extends DiscordWhitelistCommand implements SimpleCommand {

    public AddUser(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        super(api, discordOAuthHandler);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("flexbans.discord.add")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.add-user.usage"));
            return;
        }

        String playerName = args[2];

        executeCommand(source, playerName, true);
    }

    @Override
    protected void sendMessage(Object source, String message) {
        if (source instanceof CommandSource sender) {
            sender.sendMessage(LanguageManager.getMessageComponent(message));
        }
    }
}
