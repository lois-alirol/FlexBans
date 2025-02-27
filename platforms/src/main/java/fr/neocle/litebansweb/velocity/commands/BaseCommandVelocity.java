package fr.neocle.litebansweb.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.velocity.commands.SubCommands.DiscordWhitelist;
import fr.neocle.litebansweb.velocity.commands.SubCommands.PlayersWhitelist;
import fr.neocle.litebansweb.velocity.commands.SubCommands.Reload;
import fr.neocle.litebansweb.velocity.commands.SubCommands.Verify;
import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.DatabaseUtils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BaseCommandVelocity implements SimpleCommand {
    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public BaseCommandVelocity(LitebansWebAPI api, ProxyServer proxyServer, Path dataFolder, AuthenticationHandler oauth2Handler, 
                                IndexHandler indexHandler, DatabaseUtils databaseUtils, Logger logger) {
        subCommands.put("reload", new Reload(dataFolder, oauth2Handler, indexHandler, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
        subCommands.put("players", new PlayersWhitelist(api, proxyServer, dataFolder, logger));
        subCommands.put("discord", new DiscordWhitelist(api, proxyServer, dataFolder, logger));
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
            return List.of("verify", "reload", "players", "discord");
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