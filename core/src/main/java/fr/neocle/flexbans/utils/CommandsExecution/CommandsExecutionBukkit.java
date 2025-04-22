package fr.neocle.flexbans.utils.CommandsExecution;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandsExecutionBukkit implements CommandsExecution {
    private final JavaPlugin plugin;

    public CommandsExecutionBukkit(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void executeCommand(String command) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            );
        }
    }
}
