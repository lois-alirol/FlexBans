package fr.neocle.litebansweb.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import fr.neocle.litebansweb.commands.SubCommands.Velocity.Reload;
import fr.neocle.litebansweb.commands.SubCommands.Velocity.Verify;
import fr.neocle.litebansweb.handlers.IndexHandler;
import fr.neocle.litebansweb.handlers.Security.AuthenticationHandler;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BaseCommandVelocity implements SimpleCommand {
    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public BaseCommandVelocity(Path dataFolder, AuthenticationHandler oauth2Handler, IndexHandler indexHandler, DatabaseUtils databaseUtils, Logger logger) {
        subCommands.put("reload", new Reload(dataFolder, oauth2Handler, indexHandler, logger));
        subCommands.put("verify", new Verify(logger, databaseUtils));
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource source = invocation.source();

        if (args.length == 0) {
            source.sendMessage(Component.text("Available subcommands: reload, verify"));
            return;
        }

        String subCommand = args[0].toLowerCase();
        SimpleCommand command = subCommands.get(subCommand);

        if (command != null) {
            command.execute(invocation);
        } else {
            source.sendMessage(Component.text("Unknown subcommand. Available: reload, verify"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return Arrays.asList("verify", "reload");
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Arrays.asList("verify", "reload").stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }

        return Collections.emptyList();
    }
}
