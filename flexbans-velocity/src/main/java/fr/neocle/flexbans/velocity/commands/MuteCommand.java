package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.commands.punishments.mute.MuteCommandExecutor;
import fr.neocle.flexbans.commands.punishments.mute.MuteExecutor;
import fr.neocle.flexbans.velocity.commands.adapters.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.commands.helpers.punishments.VelocityMuteCommandHelper;

public class MuteCommand implements SimpleCommand {
    private final MuteCommandExecutor commandExecutor;

    public MuteCommand(MuteExecutor muteExecutor, ProxyServer proxyServer) {
        this.commandExecutor = new MuteCommandExecutor(
                muteExecutor,
                new VelocityMuteCommandHelper(proxyServer)
        );
    }

    @Override
    public void execute(Invocation invocation) {
        var wrappedInvocation = new VelocityCommandInvocation(invocation, invocation.source());
        commandExecutor.execute(wrappedInvocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation. source().hasPermission("flexbans.mute");
    }
}