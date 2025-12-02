package fr.neocle.flexbans.velocity.commands.subcommands;

import com.velocitypowered.api.command. SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.common.commands.subcommands.PlayersWhitelistCommand;
import fr.neocle.flexbans.common.commands.subcommands.whitelist.PlayersAddPlayerCommand;
import fr.neocle.flexbans.common.commands.subcommands.whitelist.PlayersRemovePlayerCommand;
import fr.neocle.flexbans.handlers. security.AuthenticationHandler;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;

import java.util.List;

public class PlayersWhitelist extends PlayersWhitelistCommand implements SimpleCommand {

    public PlayersWhitelist(FlexBansAPI api, ProxyServer proxyServer, AuthenticationHandler authenticationHandler) {
        registerSubCommand("add", new PlayersAddPlayerCommand(api, authenticationHandler));
        registerSubCommand("remove", new PlayersRemovePlayerCommand(api, authenticationHandler));
    }

    @Override
    public void execute(SimpleCommand. Invocation invocation) {
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
        return invocation.source().hasPermission("flexbans.players-whitelist");
    }
}