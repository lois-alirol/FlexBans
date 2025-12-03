package fr.neocle.flexbans.common.command.subcommand.whitelist;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.adapter.command.ICommandExecutor;
import fr.neocle.flexbans.common.adapter.command.ICommandInvocation;
import fr.neocle.flexbans.common.adapter.command.ICommandSource;
import fr.neocle.flexbans.command.whitelist. DiscordWhitelistCommand;
import fr.neocle.flexbans.handler.security.oauth. DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;

public class DiscordAddUserCommand extends DiscordWhitelistCommand implements ICommandExecutor {

    public DiscordAddUserCommand(FlexBansAPI api, DiscordOAuthHandler discordOAuthHandler) {
        super(api, discordOAuthHandler);
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();
        String[] args = invocation.getArguments();

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
        if (source instanceof ICommandSource commandSource) {
            commandSource. sendMessage(LanguageManager. getMessageComponent(message));
        }
    }
}