package fr.neocle.flexbans.utils.CommandsExecution;

import org.bukkit.Bukkit;

public class CommandsExecutionBukkit implements CommandsExecution {
    @Override
    public void executeCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
