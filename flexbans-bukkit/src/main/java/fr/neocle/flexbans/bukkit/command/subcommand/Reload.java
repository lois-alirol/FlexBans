package fr.neocle.flexbans.bukkit.command.subcommand;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.Map;

public class Reload implements CommandExecutor {
    private static final FlexLogger LOGGER = FlexLogger.get(Reload.class);

    public Reload(Path dataFolder) {
    }

    @Override
    public boolean onCommand(CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!sender.hasPermission("flexbans.reload")) {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.no-permission"));
            return true;
        }

        Map<String, Object> newConfig = ConfigManager.getConfig();

        if (newConfig != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> oauthConfig = (Map<String, Object>) newConfig.get("discord-oauth");

            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.success"));
            LOGGER.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getMessageComponent("commands.reload.fail"));
            LOGGER.error(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }

        return true;
    }
}
