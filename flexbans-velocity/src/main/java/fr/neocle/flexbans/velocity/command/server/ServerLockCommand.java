package fr.neocle.flexbans.velocity.command.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.server.ServerLockExecutorImpl;
import fr.neocle.flexbans.common.command.server.lock.ServerLockCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.server.VelocityServerLockCommandHelper;

public final class ServerLockCommand {
    private final ServerLockCommandExecutor commandExecutor;

    public ServerLockCommand(ServerLockExecutorImpl serverLockExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new ServerLockCommandExecutor(
                serverLockExecutorImpl,
                new VelocityServerLockCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("serverlock")
                .requires(source -> source.hasPermission("flexbans.serverlock.lock"))
                .executes(ctx -> {
                    commandExecutor.execute(new VelocityCommandInvocation(ctx));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            commandExecutor.suggest(new VelocityCommandInvocation(ctx))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            commandExecutor.execute(new VelocityCommandInvocation(ctx));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    public static void register(CommandManager commandManager,
                                ServerLockExecutorImpl serverLockExecutorImpl,
                                ProxyServer proxyServer) {
        ServerLockCommand command = new ServerLockCommand(serverLockExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("serverlock")
                .build();

        commandManager.register(meta, brigadier);
    }
}