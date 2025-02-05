package fr.neocle.litebansweb.commands.SubCommands.Bungee;

import fr.neocle.litebansweb.utils.DatabaseUtils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.SQLException;
import java.util.logging.Logger;

public class Verify extends Command {
    private final Logger logger;
    private final DatabaseUtils databaseUtils;

    public Verify(Logger logger, DatabaseUtils databaseUtils) {
        super("verify", "litebansweb.verify");
        this.logger = logger;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("Only players can use this command."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("litebansweb.verify")) {
            player.sendMessage(new TextComponent("You do not have permission to use this command."));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(new TextComponent("Usage: /litebansweb verify <code>"));
            return;
        }

        String code = args[0];
        String username = player.getName();

        try {
            databaseUtils.insertUsername(username, code);
            databaseUtils.setVerifiedStatusForPlayerName(username, true);
            player.sendMessage(new TextComponent("Verification successful! Your account has been verified, you can now access the punishments dashboard."));
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                player.sendMessage(new TextComponent("The verification code (" + code + ") does not exist or has already been used."));
            } else {
                player.sendMessage(new TextComponent("An error occurred while verifying your account. Please try again later."));
                logger.warning("SQL Error while verifying user: " + e.getMessage());
            }
        } catch (Exception e) {
            player.sendMessage(new TextComponent("An unexpected error occurred. Please contact an administrator."));
            logger.warning("Unexpected error: " + e.getMessage());
        }
    }
}
