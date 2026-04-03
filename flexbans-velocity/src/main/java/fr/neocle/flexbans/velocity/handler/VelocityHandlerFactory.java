package fr.neocle.flexbans.velocity.handler;

import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.platform.handler.BanPlatformHandler;
import fr.neocle.flexbans.api.platform.handler.KickPlatformHandler;
import fr.neocle.flexbans.velocity.handler.punishment.KickHandler;
import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.velocity.handler.punishment.MuteHandler;
import fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler;
import fr.neocle.flexbans.velocity.handler.punishment.WarningHandler;
import fr.neocle.flexbans.api.platform.handler.ServerLockPlatformHandler;
import fr.neocle.flexbans.velocity.handler.server.lock.ServerLockHandler;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.factory.PlatformHandlerFactory;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.broadcast.BroadcasterVelocity;
import fr.neocle.flexbans.velocity.handler.punishment.BanHandler;

public class VelocityHandlerFactory implements PlatformHandlerFactory {
    private final ProxyServer proxyServer;
    private final DatabaseUtils databaseUtils;

    public VelocityHandlerFactory(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.proxyServer = proxyServer;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public Broadcaster createBroadcaster() {
        return new BroadcasterVelocity(proxyServer);
    }

    @Override
    public BanPlatformHandler createBanHandler() {
        return new BanHandler(proxyServer, databaseUtils.getProfilesManager());
    }

    @Override
    public MutePlatformHandler createMuteHandler() {
        return new MuteHandler(proxyServer);
    }

    @Override
    public KickPlatformHandler createKickHandler() {
        return new KickHandler(proxyServer);
    }

    @Override
    public WarningPlatformHandler createWarningHandler() {
        return new WarningHandler(proxyServer);
    }

    @Override
    public ServerLockPlatformHandler createServerLockHandler() {
        return new ServerLockHandler(proxyServer);
    }
}