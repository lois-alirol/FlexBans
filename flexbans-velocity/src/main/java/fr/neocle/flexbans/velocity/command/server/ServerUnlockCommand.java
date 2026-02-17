package fr.neocle.flexbans.velocity.command.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.server.ServerUnlockExecutorImpl;
import fr.neocle.flexbans.common.command.server.unlock.ServerUnlockCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.server.VelocityServerUnlockCommandHelper;

public final class ServerUnlockCommand {
    private final ServerUnlockCommandExecutor commandExecutor;

    public ServerUnlockCommand(ServerUnlockExecutorImpl serverUnlockExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new ServerUnlockCommandExecutor(
                serverUnlockExecutorImpl,
                new VelocityServerUnlockCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("serverunlock")
                .requires(source -> source.hasPermission("flexbans.serverlock.unlock"))
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
                                ServerUnlockExecutorImpl serverUnlockExecutorImpl,
                                ProxyServer proxyServer) {
        ServerUnlockCommand command = new ServerUnlockCommand(serverUnlockExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("serverunlock")
                .build();

        commandManager.register(meta, brigadier);
    }
}