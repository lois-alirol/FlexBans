package fr.neocle.flexbans.common.command.subcommand.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr. neocle.flexbans.command.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.locale.LanguageManager;

public class DiscordRemoveUserCommand extends DiscordWhitelistCommand implements ICommandExecutor {

    public DiscordRemoveUserCommand() {
        super();
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

        if (!source.hasPermission("flexbans.discord.remove")) {
            source. sendMessage(LanguageManager. getMessageComponent("commands.no-permission"));
            return;
        }

        if (args.length != 3) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.remove-user. usage"));
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