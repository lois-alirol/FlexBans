package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.unmute.UnmuteCommandExecutor;
import fr. neocle.flexbans.commands.punishments.unmute.UnmuteExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.punishments.VelocityUnmuteCommandHelper;

import java.util.List;

public class UnmuteCommand implements SimpleCommand {
    private final UnmuteCommandExecutor commandExecutor;

    public UnmuteCommand(UnmuteExecutor unmuteExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new UnmuteCommandExecutor(
                unmuteExecutor,
                new VelocityUnmuteCommandHelper(proxyServer)
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
        return invocation.source().hasPermission("flexbans.unmute");
    }
}