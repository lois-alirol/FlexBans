package fr.neocle.flexbans.util.broadcast;

public interface Broadcaster {
    void execute(String message);
    void execute(String message, String permission);
}
