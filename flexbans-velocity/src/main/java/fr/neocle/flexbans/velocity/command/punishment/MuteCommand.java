package fr.neocle.flexbans.velocity.command.punishment;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.command.punishment.MuteExecutorImpl;
import fr.neocle.flexbans.common.command.punishment.mute.MuteCommandExecutor;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.punishment.VelocityMuteCommandHelper;

public final class MuteCommand {
    private final MuteCommandExecutor commandExecutor;

    public MuteCommand(MuteExecutorImpl muteExecutorImpl, ProxyServer proxyServer) {
        this.commandExecutor = new MuteCommandExecutor(
                muteExecutorImpl,
                new VelocityMuteCommandHelper(proxyServer)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("mute")
                .requires(source -> source.hasPermission("flexbans.mute"))
                .executes(ctx -> {
                    commandExecutor.execute(new VelocityCommandInvocation(ctx));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            commandExecutor.execute(new VelocityCommandInvocation(ctx));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    public static void register(CommandManager commandManager,
                                MuteExecutorImpl muteExecutorImpl,
                                ProxyServer proxyServer) {
        MuteCommand command = new MuteCommand(muteExecutorImpl, proxyServer);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("mute")
                .build();

        commandManager.register(meta, brigadier);
    }
}