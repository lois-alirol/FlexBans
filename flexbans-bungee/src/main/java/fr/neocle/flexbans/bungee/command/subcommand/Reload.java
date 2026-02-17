package fr.neocle.flexbans.bungee.command.subcommand;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.locale.LanguageManager;
import fr.neocle.flexbans.logger.FlexLogger;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.nio.file.Path;
import java.util.Map;

public class Reload extends Command {
    private final Path dataFolder;

    public Reload(Path dataFolder) {
        super("reload", "flexbans.reload");
        this.dataFolder = dataFolder;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender.hasPermission("flexbans.reload")) {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.no-permission"));
            return;
        }

        Map<String, Object> newConfig = ConfigManager.getConfig();

        if (newConfig != null) {

            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.success"));
            FlexLogger.info(LanguageManager.getMessageString("commands.logging.reload.success"));
        } else {
            sender.sendMessage(LanguageManager.getBungeeMessageComponent(sender, "commands.reload.fail"));
            FlexLogger.error(LanguageManager.getMessageString("commands.logging.reload.fail"));
        }
    }
}
