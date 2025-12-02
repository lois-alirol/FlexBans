package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy. ProxyServer;
import fr.neocle.flexbans.common.commands.server.lock.ServerLockCommandExecutor;
import fr. neocle.flexbans.commands.server.lock.ServerLockExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.server.VelocityServerLockCommandHelper;

import java.util.List;

public class ServerLockCommand implements SimpleCommand {
    private final ServerLockCommandExecutor commandExecutor;

    public ServerLockCommand(ServerLockExecutor serverLockExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new ServerLockCommandExecutor(
                serverLockExecutor,
                new VelocityServerLockCommandHelper(proxyServer)
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
        return invocation.source().hasPermission("flexbans.serverlock. lock");
    }
}