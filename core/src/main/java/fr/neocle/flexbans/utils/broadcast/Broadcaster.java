package fr.neocle.flexbans.utils.broadcast;

public interface Broadcaster {
    void execute(String message);
    void execute(String message, String permission);
}
