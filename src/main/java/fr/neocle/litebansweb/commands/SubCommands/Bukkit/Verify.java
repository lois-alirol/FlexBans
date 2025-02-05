package fr.neocle.litebansweb.commands.SubCommands.Bukkit;

import fr.neocle.litebansweb.utils.DatabaseUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.logging.Logger;

public class Verify implements CommandExecutor {
    private final Logger logger;
    private DatabaseUtils databaseUtils;

    public Verify(Logger logger, DatabaseUtils databaseUtils) {
        this.logger = logger;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("litebansweb.verify")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("Usage: /litebansweb verify <code>");
            return true;
        }

        String code = args[0];
        String username = player.getName();

        try {
            databaseUtils.insertUsername(username, code);
            databaseUtils.setVerifiedStatusForPlayerName(username, true);
            player.sendMessage("Verification successful! Your account has been verified.");
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                player.sendMessage("The verification code (" + code + ") does not exist or has already been used.");
            } else {
                player.sendMessage("An error occurred. Please try again later.");
                logger.warning("SQL Error: " + e.getMessage());
            }
        } catch (Exception e) {
            player.sendMessage("An unexpected error occurred.");
            logger.warning("Unexpected error: " + e.getMessage());
        }

        return true;
    }
}
