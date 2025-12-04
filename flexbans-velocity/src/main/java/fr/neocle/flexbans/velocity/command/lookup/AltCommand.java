package fr.neocle.flexbans.velocity.command.lookup;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.alt.AltCommandExecutor;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.lookup.VelocityAltCommandHelper;

import java.util.List;

public class AltCommand implements SimpleCommand {
    private final AltCommandExecutor commandExecutor;

    public AltCommand(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.commandExecutor = new AltCommandExecutor(
                databaseUtils.getProfilesManager(),
                databaseUtils.getBansManager(),
                new VelocityAltCommandHelper(proxyServer, databaseUtils)
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
        return invocation.source().hasPermission("flexbans.command.alt");
    }
}