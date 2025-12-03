package fr.neocle.flexbans.velocity.command.punishment;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.punishment.mute.MuteCommandExecutor;
import fr.neocle.flexbans.command.punishment.mute.MuteExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityMuteCommandHelper;

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