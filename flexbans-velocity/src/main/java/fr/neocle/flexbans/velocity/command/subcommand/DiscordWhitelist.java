package fr.neocle.flexbans.velocity.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import fr.neocle.flexbans.common.command.subcommand.DiscordWhitelistCommand;
import fr.neocle.flexbans.common.command.subcommand.whitelist.DiscordAddUserCommand;
import fr.neocle.flexbans.common.command.subcommand.whitelist.DiscordRemoveUserCommand;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;

public final class DiscordWhitelist extends DiscordWhitelistCommand {

    private static final DiscordWhitelist INSTANCE = new DiscordWhitelist();

    private DiscordWhitelist() {
        registerSubCommand("add", new DiscordAddUserCommand());
        registerSubCommand("remove", new DiscordRemoveUserCommand());
    }

    public static LiteralArgumentBuilder<CommandSource> createNode() {
        return BrigadierCommand.literalArgumentBuilder("discord")
                .requires(source -> source.hasPermission("flexbans.discord-whitelist"))
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