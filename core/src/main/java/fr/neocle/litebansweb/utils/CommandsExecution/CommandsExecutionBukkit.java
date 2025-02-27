package fr.neocle.litebansweb.utils.CommandsExecution;

import org.bukkit.Bukkit;

public class CommandsExecutionBukkit implements CommandsExecution {
    @Override
    public void executeCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
