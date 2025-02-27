package fr.neocle.litebansweb.velocity.commands.SubCommands.DiscordWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.DiscordWhitelistCommand;
import fr.neocle.litebansweb.locale.LanguageManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.nio.file.Path;
import java.util.logging.Logger;

public class RemoveUser extends DiscordWhitelistCommand implements SimpleCommand {
    
    public RemoveUser(LitebansWebAPI api, Path dataFolder, Logger logger) {
        super(api, dataFolder.resolve("config.yml"), logger);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("litebansweb.discord.remove")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.remove-user.usage"));
            return;
        }

        String playerName = args[2];

        executeCommand(source, playerName, false);
    }

    @Override
    protected void sendMessage(Object source, String message) {
        if (source instanceof CommandSource sender) {
            sender.sendMessage(LanguageManager.getMessageComponent(message));
        }
    }
}
