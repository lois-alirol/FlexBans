package fr.neocle.flexbans.velocity.command.punishment;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.UnmuteExecutorImpl;
import fr.neocle.flexbans.common.command.punishment.unmute.UnmuteCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityUnmuteCommandHelper;

public final class UnmuteCommand {
    private final UnmuteCommandExecutor commandExecutor;

    public UnmuteCommand(UnmuteExecutorImpl unmuteExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new UnmuteCommandExecutor(
                unmuteExecutorImpl,
                new VelocityUnmuteCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("unmute")
                .requires(source -> source.hasPermission("flexbans.unmute"))
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
                                UnmuteExecutorImpl unmuteExecutorImpl,
                                ProxyServer proxyServer) {
        UnmuteCommand command = new UnmuteCommand(unmuteExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("unmute")
                .build();

        commandManager.register(meta, brigadier);
    }
}