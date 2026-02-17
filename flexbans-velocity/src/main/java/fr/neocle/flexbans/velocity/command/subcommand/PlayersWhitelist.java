package fr.neocle.flexbans.velocity.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import fr.neocle.flexbans.common.command.subcommand.PlayersWhitelistCommand;
import fr.neocle.flexbans.common.command.subcommand.whitelist.PlayersAddPlayerCommand;
import fr.neocle.flexbans.common.command.subcommand.whitelist.PlayersRemovePlayerCommand;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;

public final class PlayersWhitelist extends PlayersWhitelistCommand {

    private static final PlayersWhitelist INSTANCE = new PlayersWhitelist();

    private PlayersWhitelist() {
        registerSubCommand("add", new PlayersAddPlayerCommand());
        registerSubCommand("remove", new PlayersRemovePlayerCommand());
    }

    public static LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("players")
                .requires(source -> source.hasPermission("flexbans.players-whitelist"))
                .executes(ctx -> {
                    INSTANCE.execute(new VelocityCommandInvocation(ctx));
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            INSTANCE.suggest(new VelocityCommandInvocation(ctx))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            INSTANCE.execute(new VelocityCommandInvocation(ctx));
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}