package fr.neocle.flexbans.velocity.commands.SubCommands.PlayersWhitelistSubArgs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.PlayersWhitelistCommand;
import fr.neocle.flexbans.handlers.Security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;

import java.util.Map;

public class RemovePlayer extends PlayersWhitelistCommand implements SimpleCommand {

    public RemovePlayer(FlexBansAPI api, AuthenticationHandler authenticationHandler) {
        super(api, authenticationHandler);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("flexbans.players.remove")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.remove-player.usage"));
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
