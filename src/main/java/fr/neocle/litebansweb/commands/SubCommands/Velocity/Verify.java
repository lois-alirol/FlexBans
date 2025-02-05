package fr.neocle.litebansweb.commands.SubCommands.Velocity;

import java.sql.SQLException;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

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
            source.sendMessage(Component.text("Only players can use this command."));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("litebansweb.verify")) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length != 2 || !args[0].equalsIgnoreCase("verify")) {
            player.sendMessage(Component.text("Usage: /litebansweb verify <code>"));
            return;
        }

        String code = args[1];
        String username = player.getUsername();

        try {
            databaseUtils.insertUsername(username, code);
            databaseUtils.setVerifiedStatusForPlayerName(username, true);
            player.sendMessage(Component.text("Verification successful! Your account has been verified, you can now access the punishments dashboard."));
        } catch (SQLException e) {
            if (e.getMessage().contains("does not exist")) {
                player.sendMessage(Component.text("The verification code ("+ code +") does not exist or has already been used."));
            } else {
                player.sendMessage(Component.text("An error occurred while verifying your account. Please try again later."));
                logger.warning("SQL Error while verifying user: " + e.getMessage());
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("An unexpected error occurred. Please contact an administrator."));
            logger.warning("Unexpected error: " + e.getMessage());
        }
    }
}
