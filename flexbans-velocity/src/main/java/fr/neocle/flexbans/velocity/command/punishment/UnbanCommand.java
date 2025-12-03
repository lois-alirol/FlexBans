package fr.neocle.flexbans.velocity.command.punishment;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.punishment.unban.UnbanCommandExecutor;
import fr.neocle.flexbans.command.punishment.unban.UnbanExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityUnbanCommandHelper;

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