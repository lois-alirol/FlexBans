package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.queries.DashboardQueries;
import fr.neocle.flexbans.utils.DurationCalculator;
import fr.neocle.flexbans.utils.HooksUtils;
import fr.neocle.flexbans.utils.ResourceLoader;
import fr.neocle.flexbans.utils.Player.PlayerHeadImage;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import litebans.api.Database;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PlayerHistoryHandler extends AbstractHandler {
    private final Logger logger;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils flexbansDatabase;

    private static final int PAGE_SIZE = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.details.player.max-per-page"));
    private int totalRecords;

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();

    public PlayerHistoryHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage, DatabaseUtils databaseUtils, Logger logger) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.flexbansDatabase = databaseUtils;
        this.logger = logger;
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

        String executor = request.getParameter("executor");
        if (executor != null && (executor.matches("^[0-9a-fA-F]{32}$") || executor.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
            executor = usernameUUIDConverters.UUIDtoUsername(executor);
        }

        StringBuilder punishmentRows = new StringBuilder();
        int totalPages;

        try {
            totalPages = getTotalPages(executor, uuid);

            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }

            int offset = (page - 1) * PAGE_SIZE;

            fetchAndAddPunishments(punishmentRows, uuid, executor, offset);

        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().write("Error: Unable to fetch punishments.");
            return;
        }

        String serverColor = (String) ConfigManager.getConfigValue("server-display.color");
        String serverColorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");

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

    private int getTotalPages(String player, String executor) throws SQLException {
        String query = DashboardQueries.buildPlayerCountQuery(usingFlexBans, player, executor);
        PreparedStatement stmt;

        if (usingFlexBans) {
            stmt = flexbansDatabase.prepareStatement(query);
        } else if (usingLiteBans) {
            stmt = Database.get().prepareStatement(query);
        } else {
            logger.severe("No database system is active.");
            return 0;
        }

        try (stmt) {
            int paramIndex = 1;

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRecords = rs.getInt(1);
                    return (int) Math.ceil((double) totalRecords / PAGE_SIZE);
                }
            }
        }
        return 0;
    }

    private void fetchAndAddPunishments(StringBuilder punishmentRows, String player, String executor, int offset) throws SQLException {
        String query = DashboardQueries.buildPlayerPunishmentsQuery(usingFlexBans, player, executor, PAGE_SIZE, offset);
        PreparedStatement stmt;

        if (usingFlexBans) {
            stmt = flexbansDatabase.prepareStatement(query);
        } else if (usingLiteBans) {
            stmt = Database.get().prepareStatement(query);
        } else {
            logger.severe("No database system is active.");
            return;
        }

        try (stmt) {
            int paramIndex = 1;

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            stmt.setInt(paramIndex++, PAGE_SIZE);
            stmt.setInt(paramIndex++, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows);
                }
            }
        }
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

        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        boolean hasRemovedByName = false;
        boolean hasRemovedByDate = false;

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            if (columnName.equalsIgnoreCase("removed_by_name")) {
                hasRemovedByName = true;
            } else if (columnName.equalsIgnoreCase("removed_by_date")) {
                hasRemovedByDate = true;
            }
        }

        String removedByName = " ";
        if (hasRemovedByName) {
            removedByName = rs.getString("removed_by_name");
            if (removedByName == null) {
                removedByName = " ";
            }
        }

        Timestamp removedByDate = null;
        if (hasRemovedByDate) {
            removedByDate = rs.getTimestamp("removed_by_date");
        }

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

        String duration = DurationCalculator.calculateDuration(time, until);

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
            punishmentRows.append("<tr onclick=\"window.location.href='/details/").append(uncapitalize(type)).append("s/").append(punishmentID).append("';\">")
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
