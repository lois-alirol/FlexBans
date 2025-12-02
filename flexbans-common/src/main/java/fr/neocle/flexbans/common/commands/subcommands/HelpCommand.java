package fr.neocle.flexbans.common.commands.subcommands;

import fr.neocle.flexbans.common.commands.ICommandExecutor;
import fr.neocle.flexbans.common.commands. ICommandInvocation;
import fr.neocle.flexbans.common.commands.ICommandSource;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class HelpCommand implements ICommandExecutor {
    private final Logger logger;

    public HelpCommand(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void execute(ICommandInvocation invocation) {
        ICommandSource source = invocation.getSource();

        if (!source.hasPermission("flexbans.help")) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        String helpMessage = LanguageManager.getMessageString("commands.help.message");
        if (helpMessage == null || helpMessage.isEmpty()) {
            source.sendMessage(Component.text("No help message found."));
            return;
        }

        String[] lines = helpMessage.split("\\n");
        MiniMessage miniMessage = MiniMessage.miniMessage();
        for (String line : lines) {
            source.sendMessage(miniMessage.deserialize(line));
        }
    }

    @Override
    public List<String> suggest(ICommandInvocation invocation) {
        return Collections.emptyList();
    }
}