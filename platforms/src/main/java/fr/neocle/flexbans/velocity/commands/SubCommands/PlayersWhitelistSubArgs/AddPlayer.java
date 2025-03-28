package fr.neocle.flexbans.velocity.commands.SubCommands.PlayersWhitelistSubArgs;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.commands.whitelist.PlayersWhitelistCommand;
import fr.neocle.flexbans.handlers.Security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;

import java.util.Map;

public class AddPlayer extends PlayersWhitelistCommand implements SimpleCommand {

    public AddPlayer(FlexBansAPI api, AuthenticationHandler authenticationHandler, Map<String, Object> config) {
        super(api, config, authenticationHandler);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("flexbans.players.add")) {
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
