package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.warning.WarningCommandExecutor;
import fr.neocle.flexbans.commands.punishments.warning.WarningExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.punishments.VelocityWarningCommandHelper;

import java.util.List;

public class WarningCommand implements SimpleCommand {
    private final WarningCommandExecutor commandExecutor;

    public WarningCommand(WarningExecutor warningExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new WarningCommandExecutor(
                warningExecutor,
                new VelocityWarningCommandHelper(proxyServer)
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
        return invocation.source().hasPermission("flexbans.warning");
    }
}