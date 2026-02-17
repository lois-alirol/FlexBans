package fr.neocle.flexbans.velocity.command.punishment;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.WarningExecutorImpl;
import fr.neocle.flexbans.common.command.punishment.warning.WarningCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityWarningCommandHelper;

public final class WarningCommand {
    private final WarningCommandExecutor commandExecutor;

    public WarningCommand(WarningExecutorImpl warningExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new WarningCommandExecutor(
                warningExecutorImpl,
                new VelocityWarningCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("warning")
                .requires(source -> source.hasPermission("flexbans.warning"))
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
                                WarningExecutorImpl warningExecutorImpl,
                                ProxyServer proxyServer) {
        WarningCommand command = new WarningCommand(warningExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("warning")
                .build();

        commandManager.register(meta, brigadier);
    }
}