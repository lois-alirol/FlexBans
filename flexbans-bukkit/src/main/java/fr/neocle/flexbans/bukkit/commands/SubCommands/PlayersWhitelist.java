package fr.neocle.flexbans.bukkit.commands.SubCommands;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.bukkit.commands.SubCommands.PlayersWhitelistSubArgs.AddPlayer;
import fr.neocle.flexbans.bukkit.commands.SubCommands.PlayersWhitelistSubArgs.RemovePlayer;
import fr.neocle.flexbans.handlers.security.AuthenticationHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayersWhitelist implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new HashMap<>();

    public PlayersWhitelist(FlexBansAPI api, AuthenticationHandler authenticationHandler) {
        subCommands.put("add", new AddPlayer(api, authenticationHandler));
        subCommands.put("remove", new RemovePlayer(api, authenticationHandler));
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
