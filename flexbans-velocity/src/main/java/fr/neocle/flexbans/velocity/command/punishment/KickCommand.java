package fr.neocle.flexbans.velocity.command.punishment;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.punishment.kick.KickCommandExecutor;
import fr. neocle.flexbans.command.punishment.kick.KickExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityKickCommandHelper;

import java.util.List;

public class KickCommand implements SimpleCommand {
    private final KickCommandExecutor commandExecutor;

    public KickCommand(KickExecutor kickExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new KickCommandExecutor(
                kickExecutor,
                new VelocityKickCommandHelper(proxyServer)
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
        return invocation.source().hasPermission("flexbans.kick");
    }
}