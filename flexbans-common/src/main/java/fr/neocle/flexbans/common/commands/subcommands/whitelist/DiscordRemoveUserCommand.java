package fr.neocle.flexbans.common.commands.subcommands.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands.ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr. neocle.flexbans. commands.whitelist.DiscordWhitelistCommand;
import fr.neocle.flexbans.handlers.security.oauth. DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;

public class DiscordRemoveUserCommand extends DiscordWhitelistCommand implements ICommandExecutor {

    public DiscordRemoveUserCommand(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        super(api, discordOAuthHandler);
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