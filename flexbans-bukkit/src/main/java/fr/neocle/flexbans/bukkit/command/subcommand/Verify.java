package fr.neocle.flexbans.bukkit.command.subcommand;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class Verify implements CommandExecutor {
    private final UserManager userManager;
    private static final FlexLogger LOGGER = FlexLogger.get(Verify.class);

    public Verify(DatabaseUtils databaseUtils) {
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command,
                             @NonNull String label, String @NonNull [] args) {
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
            boolean isRegistered = userManager.isUserRegistered(username).join();
            boolean isVerified  = userManager.isUserVerified(username).join();

            if (isRegistered && isVerified) {
                // Already registered and verified → just link Discord
                boolean discordSet = userManager.setDiscordId(username, code).join();
                if (discordSet) {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));
                } else {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                }
                return true;
            }

            if (!isRegistered) {
                // User doesn't exist → create for verification
                boolean created = userManager.createUserForVerification(username, playerUuid).join();
                if (!created) {
                    player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                    return true;
                }
            }

            // Mark as verified
            boolean verified = userManager.verifyUser(username, playerUuid).join();
            if (!verified) {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return true;
            }

            // Set Discord ID
            boolean discordSet = userManager.setDiscordId(username, code).join();
            if (!discordSet) {
                player.sendMessage(LanguageManager.getMessageComponent("commands.verify.fail"));
                return true;
            }

            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.success"));

        } catch (Exception e) {
            player.sendMessage(LanguageManager.getMessageComponent("commands.verify.error"));
            LOGGER.warn(
                    LanguageManager.getMessageString("commands.logging.verify.exception")
                    , e);
        }

        return true;
    }
}