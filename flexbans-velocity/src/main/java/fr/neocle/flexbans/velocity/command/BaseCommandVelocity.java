package fr.neocle.flexbans.velocity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.common.command.subcommand.HelpCommand;
import fr.neocle.flexbans.common.command.subcommand.ReloadCommand;
import fr.neocle.flexbans.common.command.subcommand.VerifyCommand;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.util.JettyReloader;
import fr.neocle.flexbans.velocity.command.adapter.command.VelocityCommandInvocation;
import fr.neocle.flexbans.velocity.command.subcommand.*;

import java.nio.file.Path;

public final class BaseCommandVelocity {

    public static BrigadierCommand createCommand(
            ProxyServer proxyServer,
            Path dataFolder,
            JettyReloader jettyReloader,
            DatabaseUtils databaseUtils,
            String version
    ) {

        LiteralArgumentBuilder<CommandSource> root = BrigadierCommand
                .literalArgumentBuilder("flexbans")
                .requires(src -> src.hasPermission("flexbans.use"));

        root.then(BrigadierCommand
                .literalArgumentBuilder("help")
                .executes(ctx -> {
                    new HelpCommand().execute(new VelocityCommandInvocation(ctx));
                    return Command.SINGLE_SUCCESS;
                })
        );

        root.then(BrigadierCommand
                .literalArgumentBuilder("reload")
                .executes(ctx -> {
                    new ReloadCommand(dataFolder, jettyReloader).execute(
                            new VelocityCommandInvocation(ctx)
                    );
                    return Command.SINGLE_SUCCESS;
                })
        );

        root.then(BrigadierCommand
                .literalArgumentBuilder("verify")
                .executes(ctx -> {
                    new VerifyCommand(databaseUtils.getUserManager()).execute(
                            new VelocityCommandInvocation(ctx)
                    );
                    return Command.SINGLE_SUCCESS;
                })
        );

        root.then(PlayersWhitelist.createNode());
        root.then(DiscordWhitelist.createNode());
        root.then(new Dump(proxyServer, version).createNode());

        return new BrigadierCommand(root.build());
    }

    public static void register(
            ProxyServer proxyServer,
            Path dataFolder,
            JettyReloader jettyReloader,
            DatabaseUtils databaseUtils,
            String version
    ) {
        CommandManager manager = proxyServer.getCommandManager();

        BrigadierCommand cmd = createCommand(
                proxyServer, dataFolder, jettyReloader, databaseUtils, version
        );

        CommandMeta meta = manager.metaBuilder(cmd)
                .aliases("flexbans", "fb")
                .build();

        manager.register(meta, cmd);
    }
}
