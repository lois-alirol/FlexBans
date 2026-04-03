package fr.neocle.flexbans.velocity.command.lookup;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.lookup.history.HistoryCommandExecutor;
import fr.neocle.flexbans.database.DatabaseConnectionManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.helper.lookup.VelocityHistoryCommandHelper;

public final class HistoryCommand {
    private final HistoryCommandExecutor commandExecutor;

    public HistoryCommand(ProxyServer proxyServer, DatabaseUtils databaseUtils,
                          DatabaseConnectionManager dbManager) {
        this.commandExecutor = new HistoryCommandExecutor(
                new VelocityHistoryCommandHelper(proxyServer, databaseUtils, dbManager),
                databaseUtils.getProfilesManager()
        );
    }

    public LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("history")
                .requires(source -> source.hasPermission("flexbans.command.history"))
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
        HistoryCommand command = new HistoryCommand(proxyServer, databaseUtils, dbManager);

        BrigadierCommand brigadier = new BrigadierCommand(command.createNode().build());
        CommandMeta meta = commandManager.metaBuilder(brigadier)
                .aliases("history")
                .build();

        commandManager.register(meta, brigadier);
    }
}