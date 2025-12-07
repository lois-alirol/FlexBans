package fr.neocle.flexbans.velocity.command.lookup;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy. ProxyServer;
import fr.neocle.flexbans.common.command.lookup.moderatorhistory.ModeratorHistoryCommandExecutor;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database. DatabaseUtils;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity. command.helper.lookup.VelocityModeratorHistoryCommandHelper;

import java.util.List;

public class ModeratorHistoryCommand implements SimpleCommand {
    private final ModeratorHistoryCommandExecutor commandExecutor;

    public ModeratorHistoryCommand(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                                   DatabaseConnectionManager dbManager) {
        this.commandExecutor = new ModeratorHistoryCommandExecutor(
                databaseUtils. getProfilesManager(),
                databaseUtils.getBansManager(),
                databaseUtils. getMutesManager(),
                databaseUtils.getWarningsManager(),
                databaseUtils.getKicksManager(),
                new VelocityModeratorHistoryCommandHelper(proxyServer, databaseUtils, dbManager)
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
        return invocation.source().hasPermission("flexbans.command.moderatorhistory");
    }
}