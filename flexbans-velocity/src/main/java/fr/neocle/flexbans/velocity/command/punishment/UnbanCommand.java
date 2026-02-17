package fr.neocle.flexbans.velocity.command.punishment;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.UnbanExecutorImpl;
import fr.neocle.flexbans.common.command.punishment.unban.UnbanCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityUnbanCommandHelper;

public final class UnbanCommand {
    private final UnbanCommandExecutor commandExecutor;

    public UnbanCommand(UnbanExecutorImpl unbanExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new UnbanCommandExecutor(
                unbanExecutorImpl,
                new VelocityUnbanCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("unban")
                .requires(source -> source.hasPermission("flexbans.unban"))
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
                                UnbanExecutorImpl unbanExecutorImpl,
                                ProxyServer proxyServer) {
        UnbanCommand command = new UnbanCommand(unbanExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("unban")
                .build();

        commandManager.register(meta, brigadier);
    }
}