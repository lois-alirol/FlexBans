package fr.neocle.litebansweb.handlers;

import fr.neocle.litebansweb.utils.DurationCalculator;
import fr.neocle.litebansweb.utils.ResourceLoader;
import fr.neocle.litebansweb.utils.Player.PlayerHeadImage;
import fr.neocle.litebansweb.utils.Player.UsernameUUIDConverters;
import litebans.api.Database;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.logging.Logger;

public class ModeratorHistoryHandler extends AbstractHandler {
    private final Map<String, Object> config;
    private Logger logger = Logger.getLogger("LitebansWeb");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DurationCalculator durationCalculator;

    private static final String CONSOLE_UUID = "[Console]";
    private static final int PAGE_SIZE = 20;
    private int totalRecords;

    public ModeratorHistoryHandler(Map<String, Object> config, UsernameUUIDConverters usernameUUIDConverters, DurationCalculator durationCalculator, PlayerHeadImage playerHeadImage) {
        this.config = config;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.durationCalculator = durationCalculator;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!target.startsWith("/moderator/")) {
            return;
        }

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        String moderatorIdentifier = target.replace("/moderator/", "").trim();
        if (moderatorIdentifier.isEmpty()) {
            response.getWriter().write("Error: Moderator UUID or Username is required.");
            return;
        }

        String uuid;
        if ("console".equalsIgnoreCase(moderatorIdentifier)) {
            uuid = CONSOLE_UUID;
        } else if (moderatorIdentifier.length() == 36) {
            uuid = moderatorIdentifier;
        } else {
            uuid = usernameUUIDConverters.usernameToUUID(moderatorIdentifier);
            if (uuid == null) {
                response.getWriter().write("Error: Could not find UUID for the given username.");
                return;
            }
        }

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/moderator_history.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for moderator history.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }

        int page = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            try {
                page = Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        StringBuilder punishmentRows = new StringBuilder();
        int totalPages = 0;

        String countQuery = "SELECT COUNT(*) FROM (" +
                "SELECT uuid FROM litebans_bans WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT uuid FROM litebans_mutes WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT uuid FROM litebans_warnings WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT uuid FROM litebans_kicks WHERE banned_by_uuid = ? " +
                ") AS t";

        try (PreparedStatement countStmt = Database.get().prepareStatement(countQuery)) {
            countStmt.setString(1, uuid);
            countStmt.setString(2, uuid);
            countStmt.setString(3, uuid);
            countStmt.setString(4, uuid);

            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    totalRecords = rs.getInt(1);
                    totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("SQL Exception: " + e.getMessage());
            response.getWriter().write("Error: Unable to fetch moderator history.");
            return;
        }

        if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }

        int offset = (page - 1) * PAGE_SIZE;

        String query = "SELECT id, uuid, reason, banned_by_uuid, time, until, type, ipban, removed_by_name, removed_by_date FROM (" +
                "SELECT id, uuid, reason, banned_by_uuid, time, until, 'Ban' AS type, ipban, removed_by_name, removed_by_date FROM litebans_bans WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT id, uuid, reason, banned_by_uuid, time, until, 'Mute' AS type, ipban, removed_by_name, removed_by_date FROM litebans_mutes WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT id, uuid, reason, banned_by_uuid, time, NULL AS until, 'Warning' AS type, 0 AS ipban, NULL AS removed_by_name, NULL AS removed_by_date FROM litebans_warnings WHERE banned_by_uuid = ? " +
                "UNION ALL " +
                "SELECT id, uuid, reason, banned_by_uuid, time, NULL AS until, 'Kick' AS type, 0 AS ipban, NULL AS removed_by_name, NULL AS removed_by_date FROM litebans_kicks WHERE banned_by_uuid = ? " +
                ") AS t ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET " + offset;

        try (PreparedStatement stmt = Database.get().prepareStatement(query)) {
            stmt.setString(1, uuid);
            stmt.setString(2, uuid);
            stmt.setString(3, uuid);
            stmt.setString(4, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("SQL Exception: " + e.getMessage());
            response.getWriter().write("Error: Unable to fetch moderator history.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server_display_settings");
        String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
        String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker_color", "#207dd2"));

        String totalPunishments = String.valueOf(totalRecords);
        String moderatorDescription = "Check all the punishments of " + capitalize(moderatorIdentifier) + " here. So far, they executed a total of " + totalPunishments + " punishments.";

        String pageContent = "";
        try {
            pageContent = htmlTemplate.replace("{{punishment_rows}}", punishmentRows.toString())
                .replace("{{moderator_username}}", capitalize(moderatorIdentifier))
                .replace("{{current_page}}", String.valueOf(page))
                .replace("{{total_pages}}", String.valueOf(totalPages))
                .replace("{{favicon}}", (playerHeadImage.getPlayerHeadUrl(moderatorIdentifier, "32")))
                .replace("{{moderator_description}}", moderatorDescription)
                .replace("{{moderator_icon}}", (playerHeadImage.getPlayerHeadUrl(moderatorIdentifier, "512")))
                .replace("{{server_color}}", serverColor)
                .replace("{{server_color_hover}}", serverColorDarker);
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.getWriter().write(pageContent);
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows) throws SQLException {
        String type = rs.getString("type").toLowerCase();
        String playerUUID = rs.getString("uuid");
        String reason = rs.getString("reason");
        String moderatorUUID = rs.getString("banned_by_uuid");
        long time = rs.getLong("time");
        long until = rs.getLong("until");
        boolean isIpBan = rs.getInt("ipban") == 1;
        int punishmentID = rs.getInt("id");
        
        String removedByName = rs.getString("removed_by_name");
        Timestamp removedByDate = rs.getTimestamp("removed_by_date");
    
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(time));
        String expirationDate = (until == 0 || until == -1) ? "Never" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(until));
    
        String moderatorName = moderatorUUID.equals(CONSOLE_UUID) ? "Console" : usernameUUIDConverters.UUIDtoUsername(moderatorUUID);
        String playerName = usernameUUIDConverters.UUIDtoUsername(playerUUID);
    
        if (isIpBan) {
            if (type.equals("ban")) {
                type = "IP-Ban";
            } else if (type.equals("mute")) {
                type = "IP-Mute";
            }
        } else {
            if (type.equals("ban")) {
                type = "Ban";
            } else if (type.equals("mute")) {
                type = "Mute";
            } else if (type.equals("warning")) {
                type = "Warning";
            } else if (type.equals("kick")) {
                type = "Kick";
            }
        }
    
        String duration = durationCalculator.calculateDuration(time, until);
    
        String typeColorClass = getTypeColorClass(type);
    
        boolean isExpiredByTime = until != -1 && until != 0 && until < System.currentTimeMillis();
        boolean isManuallyRemoved = removedByName != null && removedByDate != null && removedByDate.before(new java.util.Date());
        boolean isExplicitlyExpired = "#expired".equals(removedByName);
    
        String status;
        String badgeColorClass;
        if (type.equals("Kick")) {
            status = " / ";
            badgeColorClass = "flex items-center justify-center bg-gray-500 text-white";
        } else if (type.equals("Kick") && isManuallyRemoved) {
            status = "Removed";
            badgeColorClass = "flex items-center justify-center bg-orange-500 text-white";
        } else if (type.equals("Kick") && isExplicitlyExpired || isExpiredByTime) {
            status = "Expired";
            badgeColorClass = "flex items-center justify-center bg-red-500 text-white";
        } else {
            status = "Active";
            badgeColorClass = "flex items-center justify-center bg-green-500 text-white";
        }
    
        try {
            punishmentRows.append("<tr onclick=\"window.location.href='/details/").append(uncapitalize(type)).append("/").append(punishmentID).append("';\">")
                        .append("<td class='px-6 py-4 whitespace-nowrap'>")
                        .append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                        .append(typeColorClass).append("'>").append(type).append("</span>")
                        .append("</td>")
                    
                        // Player Name Column
                        .append("<td class='px-6 py-4 whitespace-nowrap'>")
                        .append("<img src='").append(playerHeadImage.getPlayerHeadUrl(playerName, "32")).append("' alt='Player Head' class='inline-block'> ")
                        .append("<a href='/player/").append(playerName).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                        .append(playerName != null ? playerName : playerUUID).append("</a>")
                        .append("</td>")
                    
                        // Moderator Name Column
                        .append("<td class='px-6 py-4 whitespace-nowrap'>")
                        .append("<img src='").append(playerHeadImage.getPlayerHeadUrl(moderatorName, "32")).append("' alt='Moderator Head' class='inline-block'> ")
                        .append("<a href='/moderator/").append(moderatorName).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                        .append(moderatorName).append("</a>")
                        .append("</td>")
                    
                        // Reason Column
                        .append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                        .append(reason)
                        .append("</td>")
                    
                        // Date Column
                        .append("<td class='px-6 py-4'>")
                        .append(date)
                        .append("</td>")
                    
                        // Expiration Date Column
                        .append("<td class='px-6 py-4'>")
                        .append(expirationDate)
                        .append("</td>")
                    
                        // Duration Column
                        .append("<td class='px-6 py-4'>")
                        .append(duration)
                        .append("</td>")
                    
                        // Status Column with Badge
                        .append("<td class='px-6 py-4 whitespace-nowrap'>")
                        .append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                        .append(badgeColorClass).append("'>").append(status).append("</span>")
                        .append("</td>")
                    
                        .append("</tr>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
    
    private String getTypeColorClass(String type) {
        switch (type) {
            case "IP-Ban":
                return "bg-red-800 text-white"; // Darker red for IP bans
            case "Ban":
                return "bg-red-600 text-white"; // Red for bans
            case "IP-Mute":
                return "bg-orange-500 text-white"; // Orange for IP mutes
            case "Mute":
                return "bg-orange-400 text-white"; // Light orange for mutes
            case "Warning":
                return "bg-yellow-400 text-black"; // Yellow for warnings
            case "Kick":
                return "bg-yellow-400 text-black"; // Yellow for kicks
            default:
                return "bg-gray-500 text-white"; // Gray for unknown types
        }
    }

    public static String capitalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        char firstChar = input.charAt(0);
        if (Character.isLetter(firstChar) && Character.isLowerCase(firstChar)) {
            return Character.toUpperCase(firstChar) + input.substring(1);
        }
        return input;
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}