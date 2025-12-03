package fr.neocle.flexbans.velocity.command.punishment;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.punishment.warning.WarningCommandExecutor;
import fr.neocle.flexbans.command.punishment.warning.WarningExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityWarningCommandHelper;

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