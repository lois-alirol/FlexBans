package fr.neocle.litebansweb.velocity.commands.SubCommands;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.velocity.commands.SubCommands.PlayersWhitelistSubArgs.AddPlayer;
import fr.neocle.litebansweb.velocity.commands.SubCommands.PlayersWhitelistSubArgs.RemovePlayer;

public class PlayersWhitelist implements SimpleCommand {
    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public PlayersWhitelist(LitebansWebAPI api, ProxyServer proxyServer, AuthenticationHandler authenticationHandler, Map<String, Object> config) {
        subCommands.put("add", new AddPlayer(api, authenticationHandler, config));
        subCommands.put("remove", new RemovePlayer(api, authenticationHandler, config));
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length == 1) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();
        SimpleCommand command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.usage"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
    
        if (args.length == 2) {
            return subCommands.keySet().stream().toList();
        }
    
        if (args.length == 3) {
            String input = args[2].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }
    
        return Collections.emptyList();
    }    
}
