package fr.neocle.flexbans.bukkit.commands.SubCommands;

import fr.neocle.flexbans.database.Dashboard.UserManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.locale.LanguageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.logging.Logger;

public class Verify implements CommandExecutor {
    private final Logger logger;
    private UserManager userManager;

    public Verify(Logger logger, DatabaseUtils databaseUtils) {
        this.logger = logger;
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("flexbans.verify")) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.usage"));
            return true;
        }

        String code = args[0];
        String username = player.getName();

        try {
            userManager.insertUsername(username, code);
            userManager.setVerifiedStatusForPlayerName(username, true);
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
            logger.warning(LanguageManager.getMessageString("commands.logging.verify.exception"));
        }

        return true;
    }
}
