package fr.neocle.litebansweb.bukkit.commands.SubCommands;

import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.neocle.litebansweb.api.LitebansWebAPI;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.PlayersWhitelistSubArgs.AddPlayer;
import fr.neocle.litebansweb.bukkit.commands.SubCommands.PlayersWhitelistSubArgs.RemovePlayer;
import fr.neocle.litebansweb.locale.LanguageManager;

public class PlayersWhitelist implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new HashMap<>();

    public PlayersWhitelist(LitebansWebAPI api, AuthenticationHandler authenticationHandler, Map<String, Object> config) {
        subCommands.put("add", new AddPlayer(api, authenticationHandler, config));
        subCommands.put("remove", new RemovePlayer(api, authenticationHandler, config));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        CommandExecutor executor = subCommands.get(subCommand);

        if (executor != null) {
            return executor.onCommand(sender, command, label, args);
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.players-whitelist.usage"));
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands.keySet()) {
                if (subCommand.startsWith(input)) {
                    suggestions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            CommandExecutor subExecutor = subCommands.get(args[0].toLowerCase());
            if (subExecutor instanceof TabCompleter) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                List<String> subSuggestions = ((TabCompleter) subExecutor).onTabComplete(sender, command, alias, subArgs);
                if (subSuggestions != null) {
                    suggestions.addAll(subSuggestions);
                }
            }
        }

        return suggestions;
    }
}
