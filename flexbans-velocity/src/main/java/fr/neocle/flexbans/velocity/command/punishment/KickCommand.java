package fr.neocle.flexbans.velocity.command.punishment;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.KickExecutorImpl;
import fr.neocle.flexbans.common.command.punishment.kick.KickCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityKickCommandHelper;

public final class KickCommand {
    private final KickCommandExecutor commandExecutor;

    public KickCommand(KickExecutorImpl kickExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new KickCommandExecutor(
                kickExecutorImpl,
                new VelocityKickCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("kick")
                .requires(source -> source.hasPermission("flexbans.kick"))
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
                                KickExecutorImpl kickExecutorImpl,
                                ProxyServer proxyServer) {
        KickCommand command = new KickCommand(kickExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("kick")
                .build();

        commandManager.register(meta, brigadier);
    }
}