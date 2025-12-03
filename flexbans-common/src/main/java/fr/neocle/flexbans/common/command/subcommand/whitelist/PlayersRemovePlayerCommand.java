package fr.neocle.flexbans.common.command.subcommand.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr. neocle.flexbans.command.whitelist.PlayersWhitelistCommand;
import fr. neocle.flexbans.handler.security.AuthenticationHandler;
import fr.neocle. flexbans.locale.LanguageManager;

public class PlayersRemovePlayerCommand extends PlayersWhitelistCommand implements ICommandExecutor {

    public PlayersRemovePlayerCommand(FlexBansAPI api, AuthenticationHandler authenticationHandler) {
        super(api, authenticationHandler);
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (!source.hasPermission("flexbans.players.remove")) {
            source. sendMessage(LanguageManager. getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.remove-player. usage"));
            return;
        }

        String playerName = args[2];
        executeCommand(source, playerName, false);
    }

    @Override
    protected void sendMessage(Object source, String message) {
        if (source instanceof ICommandSource commandSource) {
            commandSource.sendMessage(LanguageManager.getMessageComponent(message));
        }
    }
}