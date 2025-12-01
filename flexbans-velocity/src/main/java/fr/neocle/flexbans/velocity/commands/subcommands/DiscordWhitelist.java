package fr.neocle.flexbans.velocity.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.neocle.flexbans.api.FlexBansAPI;
import fr.neocle.flexbans.handlers.security.oauth.DiscordOAuthHandler;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.velocity.commands.subcommands.whitelist.discord.AddUser;
import fr.neocle.flexbans.velocity.commands.subcommands.whitelist.discord.RemoveUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordWhitelist implements SimpleCommand {
    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public DiscordWhitelist(FlexBansAPI api, ProxyServer proxyServer, DiscordOAuthHandler discordOAuthHandler) {
        subCommands.put("add", new AddUser(api, discordOAuthHandler));
        subCommands.put("remove", new RemoveUser(api, discordOAuthHandler));
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length == 1) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.usage"));
            return;
        }

        String subCommand = args[1].toLowerCase();
        SimpleCommand command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(LanguageManager.getMessageComponent("commands.discord-whitelist.usage"));
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
