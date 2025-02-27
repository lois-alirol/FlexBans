package fr.neocle.litebansweb.velocity.commands.SubCommands;

import java.sql.SQLException;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import fr.neocle.litebansweb.locale.LanguageManager;
import fr.neocle.litebansweb.utils.DatabaseUtils;
import net.kyori.adventure.text.Component;

public class Verify implements SimpleCommand {
    private final Logger logger;
    private final DatabaseUtils databaseUtils;

    public Verify(Logger logger, DatabaseUtils databaseUtils) {
        this.logger = logger;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(LanguageManager.getMessageComponent("commands.players-only"));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("litebansweb.verify")) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 2 || !args[0].equalsIgnoreCase("verify")) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.usage"));
            return;
        }

        String code = args[1];
        String username = player.getUsername();

        try {
            databaseUtils.insertUsername(username, code);
            databaseUtils.setVerifiedStatusForPlayerName(username, true);
            databaseUtils.printEntireDatabase();
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.unknown-code").replaceText(builder -> builder.matchLiteral("%code%").replacement(Component.text(code))));
            } else {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                logger.warning(LanguageManager.getMessageString("commands.logging.verify.sql-exception").replace("%error%", e.getMessage()));
            }
        } catch (Exception e) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.error"));
            logger.warning(LanguageManager.getMessageString("commands.logging.verify.exception").replace("%error%", e.getMessage()));
        }
    }
}
