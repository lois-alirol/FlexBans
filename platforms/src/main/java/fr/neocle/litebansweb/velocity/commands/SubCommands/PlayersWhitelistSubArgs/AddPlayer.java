package fr.neocle.litebansweb.velocity.commands.SubCommands.PlayersWhitelistSubArgs;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.commands.PlayersWhitelistCommand;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class AddPlayer extends PlayersWhitelistCommand implements SimpleCommand {
    
    public AddPlayer(LitebansWebAPI api, AuthenticationHandler authenticationHandler, Map<String, Object> config) {
        super(api, config, authenticationHandler);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("litebansweb.players.add")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.add-player.usage"));
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
