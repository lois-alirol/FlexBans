package fr.neocle.flexbans.handler.factory;

import fr.neocle.flexbans.api.platform.handler.BanPlatformHandler;
import fr.neocle.flexbans.api.platform.handler.KickPlatformHandler;
import fr.neocle.flexbans.api.platform.handler.MutePlatformHandler;
import fr.neocle.flexbans.api.platform.handler.WarningPlatformHandler;
import fr.neocle.flexbans.api.platform.handler.ServerLockPlatformHandler;
import fr.neocle.flexbans.util.broadcast.Broadcaster;
import fr.neocle.flexbans.util.commandsexecution.CommandsExecution;

public interface PlatformHandlerFactory {
    CommandsExecution createCommandsExecution();

    Broadcaster createBroadcaster();

    BanPlatformHandler createBanHandler();

    MutePlatformHandler createMuteHandler();

    KickPlatformHandler createKickHandler();

    WarningPlatformHandler createWarningHandler();

    ServerLockPlatformHandler createServerLockHandler();
}
