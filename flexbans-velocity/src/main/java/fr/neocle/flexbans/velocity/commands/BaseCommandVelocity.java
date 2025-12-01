package fr.neocle.flexbans.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.IndexHandler;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.utils.JettyReloader;
import fr.neocle.flexbans.velocity.commands.subcommands.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BaseCommandVelocity implements SimpleCommand {
    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public BaseCommandVelocity(FlexBansAPI api, ProxyServer proxyServer, Path dataFolder, AuthenticationHandler authenticationHandler, DiscordOAuthHandler discordOAuthHandler,
                               IndexHandler indexHandler, JettyReloader jettyReloader, DatabaseUtils databaseUtils, String version, Logger logger) {
        subCommands.put("help", new Help(logger));
        subCommands.put("reload", new Reload(dataFolder, authenticationHandler, indexHandler, jettyReloader, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
        subCommands.put("players", new PlayersWhitelist(api, proxyServer, authenticationHandler));
        subCommands.put("discord", new DiscordWhitelist(api, proxyServer, discordOAuthHandler));
        subCommands.put("dump", new Dump(proxyServer, version));
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length == 0) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        SimpleCommand command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return List.of("help", "verify", "reload", "players", "discord", "dump");
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .toList();
        }

        String subCommand = args[0].toLowerCase();
        SimpleCommand command = subCommands.get(subCommand);

        if (command != null) {
            return command.suggest(invocation);
        }

        return Collections.emptyList();
    }
}