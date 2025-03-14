package fr.neocle.litebansweb.velocity.commands.SubCommands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.neocle.litebansweb.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.logging.Logger;

public class Help implements SimpleCommand {
    private final Logger logger;

    public Help(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission("litebansweb.help")) {
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
}
