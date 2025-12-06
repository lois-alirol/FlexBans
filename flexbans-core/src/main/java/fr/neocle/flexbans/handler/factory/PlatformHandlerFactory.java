package fr.neocle.flexbans.handler.factory;

import fr.neocle.flexbans.command.punishment.ban.BanPlatformHandler;
import fr.neocle.flexbans.command.punishment.kick.KickPlatformHandler;
import fr.neocle.flexbans.command.punishment.mute.MutePlatformHandler;
import fr.neocle.flexbans.command.punishment.warning.WarningPlatformHandler;
import fr.neocle.flexbans.command.server.lock.ServerLockPlatformHandler;
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
