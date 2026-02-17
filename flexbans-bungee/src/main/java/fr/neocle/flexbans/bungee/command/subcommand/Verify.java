package fr.neocle.flexbans.bungee.command.subcommand;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.UUID;

public class Verify extends Command {
    private final UserManager userManager;

    public Verify(DatabaseUtils databaseUtils) {
        super("verify", "flexbans.verify");
        this.userManager = databaseUtils.getUserManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.players-only"));
            return;
        }

        if (!player.hasPermission("flexbans.verify")) {
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.usage"));
            return;
        }

        String code = args[0];
        String username = player.getName();
        UUID playerUuid = player.getUniqueId();

        try {
            boolean isRegistered = userManager.isUserRegistered(username);
            boolean isVerified = userManager.isUserVerified(username);

            if (isRegistered && isVerified) {
                // Already registered & verified → just link Discord
                boolean success = userManager.setDiscordId(username, code);
                if (success) {
                    player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.success"));
                } else {
                    player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
                }
                return;
            }

            if (!isRegistered) {
                // Create user for verification
                if (!userManager.createUserForVerification(username, playerUuid)) {
                    player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
                    return;
                }
            }

            // Verify the user
            if (!userManager.verifyUser(username, playerUuid)) {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
                return;
            }

            // Link Discord ID
            if (!userManager.setDiscordId(username, code)) {
                player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.fail"));
                return;
            }

            player.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.verify.success"));

        } catch (Exception e) {
            // Handle unknown code or SQL issues
            String messageKey = e.getMessage() != null && e.getMessage().contains("does not exist")
                    ? "commands.verify.unknown-code"
                    : "commands.verify.error";

            BaseComponent[] components = LanguageManager.getBungeeMessageComponent(sender, messageKey);

            if ("commands.verify.unknown-code".equals(messageKey)) {
                // Replace %code% placeholder
                components = Arrays.stream(components)
                        .map(c -> c instanceof TextComponent tc
                                ? new TextComponent(tc.getText().replace("%code%", code))
                                : c)
                        .toArray(BaseComponent[]::new);
            }

            player.sendMessage(components);
            FlexLogger.warn(LanguageManager.getMessageString("commands.logging.verify.exception")
                    .replace("%error%", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
