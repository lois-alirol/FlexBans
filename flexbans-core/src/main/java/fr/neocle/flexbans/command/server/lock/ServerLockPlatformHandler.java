package fr.neocle.flexbans.command.server.lock;

public interface ServerLockPlatformHandler {
    void applyLock(String serverName, String reason, String senderName, long date, String duration);
}