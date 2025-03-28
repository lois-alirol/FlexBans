package fr.neocle.flexbans.bukkit.commands;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.bukkit.commands.SubCommands.DiscordWhitelist;
import fr.neocle.flexbans.bukkit.commands.SubCommands.PlayersWhitelist;
import fr.neocle.flexbans.bukkit.commands.SubCommands.Reload;
import fr.neocle.flexbans.bukkit.commands.SubCommands.Verify;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handlers.IndexHandler;
import fr.neocle.flexbans.handlers.Security.AuthenticationHandler;
import fr.neocle.flexbans.handlers.Security.OAuthHandlers.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;
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

public class BaseCommandBukkit implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new HashMap<>();

    public BaseCommandBukkit(FlexBansAPI api, Path dataFolder, AuthenticationHandler authenticationHandler, DiscordOAuthHandler discordOAuthHandler, IndexHandler indexHandler, DatabaseUtils databaseUtils, Logger logger, Map<String, Object> config) {
        subCommands.put("reload", new Reload(dataFolder, authenticationHandler, indexHandler, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
        subCommands.put("players", new PlayersWhitelist(api, authenticationHandler, config));
        subCommands.put("discord", new DiscordWhitelist(api, discordOAuthHandler, config));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        CommandExecutor subCommand = subCommands.get(subCommandName);

        if (subCommand != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            return subCommand.onCommand(sender, command, label, subArgs);
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.unknown"));
        }

        return true;
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
            CommandExecutor subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand instanceof TabCompleter) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                return ((TabCompleter) subCommand).onTabComplete(sender, command, alias, subArgs);
            }
        }

        return suggestions;
    }
}
