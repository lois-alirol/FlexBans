package fr.neocle.flexbans.velocity. commands. subcommands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.commands.subcommands.DiscordWhitelistCommand;
import fr.neocle.flexbans.common.commands.subcommands.whitelist.DiscordAddUserCommand;
import fr.neocle.flexbans.common.commands.subcommands.whitelist.DiscordRemoveUserCommand;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;

import java.util.List;

public class DiscordWhitelist extends DiscordWhitelistCommand implements SimpleCommand {

    public DiscordWhitelist(FlexBansAPI api, ProxyServer proxyServer, DiscordOAuthHandler discordOAuthHandler) {
        registerSubCommand("add", new DiscordAddUserCommand(api, discordOAuthHandler));
        registerSubCommand("remove", new DiscordRemoveUserCommand(api, discordOAuthHandler));
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        var wrappedInvocation = new VelocityCommandInvocation(invocation, invocation.source());
        super.execute(wrappedInvocation);
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        var wrappedInvocation = new VelocityCommandInvocation(invocation, invocation.source());
        return super.suggest(wrappedInvocation);
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("flexbans.discord-whitelist");
    }
}