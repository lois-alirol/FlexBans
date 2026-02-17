package fr.neocle.flexbans.bungee.command;

import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.bungee.command.subcommand.DiscordWhitelist;
import fr.neocle.flexbans.bungee.command.subcommand.PlayersWhitelist;
import fr.neocle.flexbans.bungee.command.subcommand.Reload;
import fr.neocle.flexbans.bungee.command.subcommand.Verify;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseCommandBungee extends Command implements TabExecutor {
    private final Map<String, Command> subCommands = new HashMap<>();

    public BaseCommandBungee(FlexBansAPI api, Path dataFolder,
                             DatabaseUtils databaseUtils) {

        super("flexbans", "flexbans.verify", "fb");

        subCommands.put("reload", new Reload(dataFolder));
        subCommands.put("verify", new Verify(databaseUtils));
        subCommands.put("players", new PlayersWhitelist());
        subCommands.put("discord", new DiscordWhitelist());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.unknown"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        Command command = subCommands.get(subCommand);

        if (command != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            command.execute(sender, subArgs);
        } else {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.unknown"));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands.keySet()) {
                if (subCommand.startsWith(input)) {
                    suggestions.add(subCommand);
                }
            }
        } else if (args.length > 1) {
            Command subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand instanceof TabExecutor) {
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);
                Iterable<String> subSuggestions = ((TabExecutor) subCommand).onTabComplete(sender, subArgs);
                if (subSuggestions != null) {
                    subSuggestions.forEach(suggestions::add);
                }
            }
        }

        return suggestions;
    }

}
