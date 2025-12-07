package fr.neocle.flexbans.common.command.lookup.moderatorhistory;

import java.util.List;
import java.util.UUID;

public interface IModeratorHistoryCommandHelper {
    UUID getModeratorUuid(String moderatorName);
    List<ModeratorHistoryEntry> getModeratorHistory(UUID moderatorUuid);
    String formatTime(long timestamp);
    List<String> getOnlineModerators(String partialName);
}