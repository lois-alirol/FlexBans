package fr.neocle.flexbans.bukkit.command.subcommand;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class Verify implements CommandExecutor {
    private final UserManager userManager;

    public Verify(DatabaseUtils databaseUtils) {
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.players-only"));
            return true;
        }

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
        UUID playerUuid = player.getUniqueId();

        try {
            boolean isRegistered = userManager.isUserRegistered(username);
            boolean isVerified = userManager.isUserVerified(username);

            if (isRegistered && isVerified) {
                // Already registered and verified → just link Discord
                if (userManager.setDiscordId(username, code)) {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
                } else {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                }
                return true;
            }

            if (!isRegistered) {
                // User doesn't exist → create for verification
                if (!userManager.createUserForVerification(username, playerUuid)) {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                    return true;
                }
            }

            // Mark as verified
            if (!userManager.verifyUser(username, playerUuid)) {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return true;
            }

            // Set Discord ID
            if (!userManager.setDiscordId(username, code)) {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return true;
            }

            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));

        } catch (Exception e) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.error"));
            FlexLogger.warn(LanguageManager.getMessageString("commands.logging.verify.exception"));
        }

        return true;
    }
}
