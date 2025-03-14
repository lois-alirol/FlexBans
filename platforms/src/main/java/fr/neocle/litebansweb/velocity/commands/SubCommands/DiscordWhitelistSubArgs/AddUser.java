package fr.neocle.litebansweb.velocity.commands.SubCommands.DiscordWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.DiscordWhitelistCommand;
import fr.neocle.litebansweb.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.litebansweb.locale.LanguageManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class AddUser extends DiscordWhitelistCommand implements SimpleCommand {

    public AddUser(LitebansWebAPI api, DiscordOAuthHandler discordOAuthHandler, Map<String, Object> config) {
        super(api, config, discordOAuthHandler);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("litebansweb.discord.add")) {
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
