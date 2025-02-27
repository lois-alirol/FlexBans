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

public class PlayerHistoryHandler extends AbstractHandler {
    private final Map<String, Object> config;
    private Logger logger = Logger.getLogger("LitebansWeb");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DurationCalculator durationCalculator;

    private static final int PAGE_SIZE = 20;
    private int totalRecords;

    public PlayerHistoryHandler(Map<String, Object> config, UsernameUUIDConverters usernameUUIDConverters, DurationCalculator durationCalculator, PlayerHeadImage playerHeadImage) {
        this.config = config;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.durationCalculator = durationCalculator;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!target.startsWith("/player/")) {
            return;
        }

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        String playerIdentifier = target.replace("/player/", "").trim();
        if (playerIdentifier.isEmpty()) {
            response.getWriter().write("Error: Player UUID or Username is required.");
            return;
        }

        String uuid;
        if (playerIdentifier.length() == 36) {
            uuid = playerIdentifier;
        } else {
            uuid = usernameUUIDConverters.usernameToUUID(playerIdentifier);
            if (uuid == null) {
                response.getWriter().write("Error: Could not find UUID for the given username.");
                return;
            }
        }

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/player_history.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for player history page.");
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

        try {
            String countQuery = "SELECT COUNT(*) FROM (" +
                    "SELECT uuid FROM {bans} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT uuid FROM {mutes} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT uuid FROM {warnings} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT uuid FROM {kicks} WHERE uuid = ? " +
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
            }

            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }

            int offset = (page - 1) * PAGE_SIZE;

            String query = "SELECT id, uuid, reason, banned_by_name, time, until, type, ipban, removed_by_name, removed_by_date FROM (" +
                    "SELECT id, uuid, reason, banned_by_name, time, until, 'Ban' AS type, ipban, removed_by_name, removed_by_date FROM {bans} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT id, uuid, reason, banned_by_name, time, until, 'Mute' AS type, ipban, removed_by_name, removed_by_date FROM {mutes} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT id, uuid, reason, banned_by_name, time, NULL AS until, 'Warning' AS type, 0 AS ipban, NULL AS removed_by_name, NULL AS removed_by_date FROM {warnings} WHERE uuid = ? " +
                    "UNION ALL " +
                    "SELECT id, uuid, reason, banned_by_name, time, NULL AS until, 'Kick' AS type, 0 AS ipban, NULL AS removed_by_name, NULL AS removed_by_date FROM {kicks} WHERE uuid = ? " +
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
            }

        } catch (SQLException e) {
            e.printStackTrace();
            logger.severe("SQL Exception: " + e.getMessage());
            response.getWriter().write("Error: Unable to fetch player history.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server_display_settings");
        String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
        String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker_color", "#207dd2"));
        String totalPunishments = String.valueOf(totalRecords);

        String playerDescription = "Check all the punishments of " + capitalize(playerIdentifier) + " here. So far, they received a total of " + totalPunishments + " punishments.";
        String pageContent;
        try {
            pageContent = htmlTemplate.replace("{{punishment_rows}}", punishmentRows.toString())
                                             .replace("{{player_name}}", capitalize(playerIdentifier))
                                             .replace("{{current_page}}", String.valueOf(page))
                                             .replace("{{total_pages}}", String.valueOf(totalPages))
                                             .replace("{{favicon}}", (playerHeadImage.getPlayerHeadUrl(playerIdentifier, "32")))
                                             .replace("{{player_description}}", playerDescription)
                                             .replace("{{player_icon}}", (playerHeadImage.getPlayerHeadUrl(playerIdentifier, "512")))
                                             .replace("{{server_color}}", serverColor)
                                             .replace("{{server_color_hover}}", serverColorDarker);
        } catch (Exception e) {
            pageContent = "Error: Unable to generate player history page.";
            e.printStackTrace();
        }

        response.getWriter().write(pageContent);
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows) throws SQLException {
        String type = rs.getString("type").toLowerCase();
        String playerUUID = rs.getString("uuid");
        String reason = rs.getString("reason");
        String executorName = rs.getString("banned_by_name");
        long time = rs.getLong("time");
        long until = rs.getLong("until");
        boolean isIpBan = rs.getInt("ipban") == 1;
        int punishmentID = rs.getInt("id");

        String removedByName = rs.getString("removed_by_name");
        Timestamp removedByDate = rs.getTimestamp("removed_by_date");

        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(time));
        String expirationDate = (until == 0 || until == -1) ? "Never" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(until));

        String playerName = usernameUUIDConverters.UUIDtoUsername(playerUUID);
        String executor = executorName != null ? executorName : "Console";

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
                        // Type Column with Badge
                        .append("<td>").append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                        .append(typeColorClass).append("'>").append(type).append("</span>").append("</td>")

                        // Player Name Column
                        .append("<td>").append("<img src='").append(playerHeadImage.getPlayerHeadUrl(playerName, "32")).append("' alt='Player Head' class='inline-block'> ")
                        .append("<a href='/player/").append(playerName != null ? playerName : playerUUID).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                        .append(playerName != null ? playerName : playerUUID).append("</a>")
                        .append("</td>")

                        // Executor Name Column
                        .append("<td>").append("<img src='").append(playerHeadImage.getPlayerHeadUrl(executor, "32")).append("' alt='Executor Head' class='inline-block'> ")
                        .append("<a href='/moderator/").append(executor).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                        .append(executor).append("</a>")
                        .append("</td>")

                        // Reason Column
                        .append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                        .append(reason)
                        .append("</td>")

                        // Date Column
                        .append("<td>").append(date).append("</td>")

                        // Expiration Date Column
                        .append("<td>").append(expirationDate).append("</td>")

                        // Duration Column
                        .append("<td>").append(duration).append("</td>")

                        // Status Column with Badge
                        .append("<td>")
                        .append("<span class='flex items-center justify-center px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
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
                return "flex items-center justify-center bg-red-800 text-white"; // Darker red for IP bans
            case "Ban":
                return "flex items-center justify-center bg-red-600 text-white"; // Red for bans
            case "IP-Mute":
                return "flex items-center justify-center bg-orange-500 text-white"; // Orange for IP mutes
            case "Mute":
                return "flex items-center justify-center bg-orange-400 text-white"; // Light orange for mutes
            case "Warning":
                return "flex items-center justify-center bg-yellow-400 text-black"; // Yellow for warnings
            case "Kick":
                return "flex items-center justify-center bg-yellow-400 text-black"; // Yellow for kicks
            default:
                return "flex items-center justify-center bg-gray-500 text-white"; // Gray for unknown types
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
