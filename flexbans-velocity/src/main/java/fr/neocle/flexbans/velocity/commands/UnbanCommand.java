package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.unban.UnbanCommandExecutor;
import fr.neocle.flexbans.commands.punishments.unban.UnbanExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.punishments.VelocityUnbanCommandHelper;

import java. util.List;

public class UnbanCommand implements SimpleCommand {
    private final UnbanCommandExecutor commandExecutor;

    public UnbanCommand(UnbanExecutor unbanExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new UnbanCommandExecutor(
                unbanExecutor,
                new VelocityUnbanCommandHelper(proxyServer)
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
        return commandExecutor.  suggest(wrappedInvocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("flexbans.unban");
    }
}