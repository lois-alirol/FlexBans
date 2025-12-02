package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy. ProxyServer;
import fr.neocle.flexbans.commands.punishments.ban.BanExecutor;
import fr.neocle.flexbans.common.commands.punishments.ban.BanCommandExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.punishments.VelocityBanCommandHelper;

import java.util.List;

public class BanCommand implements SimpleCommand {
    private final BanCommandExecutor commandExecutor;

    public BanCommand(BanExecutor banExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new BanCommandExecutor(
                banExecutor,
                new VelocityBanCommandHelper(proxyServer)
        );
    }

    @Override
    public void execute(Invocation invocation) {
        var wrappedInvocation = new VelocityCommandInvocation(invocation, invocation.source());
        commandExecutor.execute(wrappedInvocation);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var wrappedInvocation = new VelocityCommandInvocation(invocation, invocation.source());
        return commandExecutor. suggest(wrappedInvocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.ban");
    }
}