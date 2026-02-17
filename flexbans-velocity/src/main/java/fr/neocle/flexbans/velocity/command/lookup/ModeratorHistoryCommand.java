package fr.neocle.flexbans.velocity.command.lookup;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.moderatorhistory.ModeratorHistoryCommandExecutor;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.lookup.VelocityModeratorHistoryCommandHelper;

public final class ModeratorHistoryCommand {
    private final ModeratorHistoryCommandExecutor commandExecutor;

    public ModeratorHistoryCommand(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                                   DatabaseConnectionManager dbManager) {
        this.commandExecutor = new ModeratorHistoryCommandExecutor(
                new VelocityModeratorHistoryCommandHelper(proxyServer, databaseUtils, dbManager)
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("mhistory")
                .requires(source -> source.hasPermission("flexbans.command.moderatorhistory"))
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
                                DatabaseUtils databaseUtils,
                                DatabaseConnectionManager dbManager) {
        ModeratorHistoryCommand command = new ModeratorHistoryCommand(proxyServer, databaseUtils, dbManager);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("mhistory")
                .build();

        commandManager.register(meta, brigadier);
    }
}