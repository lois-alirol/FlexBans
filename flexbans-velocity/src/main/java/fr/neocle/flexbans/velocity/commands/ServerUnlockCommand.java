package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.server.unlock.ServerUnlockCommandExecutor;
import fr.neocle.flexbans.commands.server.unlock.ServerUnlockExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.server.VelocityServerUnlockCommandHelper;

import java.util.List;

public class ServerUnlockCommand implements SimpleCommand {
    private final ServerUnlockCommandExecutor commandExecutor;

    public ServerUnlockCommand(ServerUnlockExecutor serverUnlockExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new ServerUnlockCommandExecutor(
                serverUnlockExecutor,
                new VelocityServerUnlockCommandHelper(proxyServer)
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
        return invocation.source().hasPermission("flexbans.serverlock.unlock");
    }
}