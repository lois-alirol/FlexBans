package fr.neocle.flexbans.api.platform.handler;

public interface ServerLockPlatformHandler {
    void applyLock(String serverName, String reason, String senderName, long date, String duration);
}