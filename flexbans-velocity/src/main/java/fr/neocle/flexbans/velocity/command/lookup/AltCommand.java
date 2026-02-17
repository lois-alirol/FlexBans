package fr.neocle.flexbans.velocity.command.lookup;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.alt.AltCommandExecutor;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.lookup.VelocityAltCommandHelper;

public final class AltCommand {
    private final AltCommandExecutor commandExecutor;

    public AltCommand(ProxyServer proxyServer, DatabaseUtils databaseUtils) {
        this.commandExecutor = new AltCommandExecutor(
                databaseUtils.getProfilesManager(),
                databaseUtils.getPunishmentsManager(),
                new VelocityAltCommandHelper(proxyServer, databaseUtils)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("alt")
                .requires(source -> source.hasPermission("flexbans.command.alt"))
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
                                ProxyServer proxyServer,
                                DatabaseUtils databaseUtils) {
        AltCommand command = new AltCommand(proxyServer, databaseUtils);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("alt")
                .build();

        commandManager.register(meta, brigadier);
    }
}