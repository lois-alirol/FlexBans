package fr.neocle.flexbans.velocity.handler;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.command.punishment.ban.platform.VelocityBan;
import fr.neocle.flexbans.command.punishment.kick.KickPlatformHandler;
import fr.neocle.flexbans.command.punishment.kick.platform.VelocityKick;
import fr.neocle.flexbans.command.punishment.mute.MutePlatformHandler;
import fr.neocle.flexbans.command.punishment.mute.platform.VelocityMute;
import fr.neocle.flexbans.command.punishment.warning.WarningPlatformHandler;
import fr.neocle.flexbans.command.punishment.warning.platform.VelocityWarning;
import fr.neocle.flexbans.command.server.lock.ServerLockPlatformHandler;
import fr.neocle.flexbans.command.server.lock.platform.VelocityServerLock;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.broadcast.BroadcasterVelocity;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecutionVelocity;

public class VelocityHandlerFactory implements PlatformHandlerFactory {
    private final ProxyServer proxyServer;
    private final DatabaseUtils databaseUtils;

    public VelocityHandlerFactory(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.proxyServer = proxyServer;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public CommandsExecution createCommandsExecution() {
        return new CommandsExecutionVelocity(proxyServer);
    }

    @Override
    public Broadcaster createBroadcaster() {
        return new BroadcasterVelocity(proxyServer);
    }

    @Override
    public BanPlatformHandler createBanHandler() {
        return new VelocityBan(proxyServer, databaseUtils.getProfilesManager());
    }

    @Override
    public MutePlatformHandler createMuteHandler() {
        return new VelocityMute(proxyServer);
    }

    @Override
    public KickPlatformHandler createKickHandler() {
        return new VelocityKick(proxyServer);
    }

    @Override
    public WarningPlatformHandler createWarningHandler() {
        return new VelocityWarning(proxyServer);
    }

    @Override
    public ServerLockPlatformHandler createServerLockHandler() {
        return new VelocityServerLock(proxyServer);
    }
}