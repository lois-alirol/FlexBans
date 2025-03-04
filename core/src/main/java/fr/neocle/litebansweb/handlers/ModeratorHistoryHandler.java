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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ModeratorHistoryHandler extends AbstractHandler {
    private final Map<String, Object> config;
    private Logger logger = Logger.getLogger("LitebansWeb");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DurationCalculator durationCalculator;

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
            response.sendRedirect("/index");;
            return;
        }

        String moderatorUsername = moderatorIdentifier;
        if (moderatorIdentifier != null && moderatorIdentifier.equalsIgnoreCase("[console]")) {
            moderatorUsername = "Console";
        } else if (moderatorIdentifier != null && (moderatorIdentifier.matches("^[0-9a-fA-F]{32}$") || moderatorIdentifier.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
            moderatorUsername = usernameUUIDConverters.UUIDtoUsername(moderatorIdentifier);
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

        String player = request.getParameter("player");
        if (player != null && (!player.matches("^[0-9a-fA-F]{32}$") || !player.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
            player = usernameUUIDConverters.usernameToUUID(player);
        }

        String type = request.getParameter("type");
        if (type == null || type.isEmpty()) {
            type = "all";
        }

        String status = request.getParameter("status");
        String on = request.getParameter("on");
        String before = request.getParameter("before");
        String after = request.getParameter("after");

        StringBuilder punishmentRows = new StringBuilder();
        int totalPages;

        try {
            totalPages = getTotalPages(moderatorUsername, player, type, status, on, before, after);

            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }

            int offset = (page - 1) * PAGE_SIZE;

            String query = buildQuery(player, type, status, on, before, after, offset);
            fetchAndAddPunishments(query, punishmentRows, moderatorUsername, player, status, on, before, after);

        } catch (SQLException e) {
            e.printStackTrace();
            response.getWriter().write("Error: Unable to fetch punishments.");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server-display");
        String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
        String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker-color", "#207dd2"));

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

    private int getTotalPages(String executor, String player, String type, String status, String on, String before, String after) throws SQLException {
        StringBuilder baseQuery = new StringBuilder("SELECT COUNT(*) FROM (");
        List<String> queries = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("ban")) {
            queries.add("SELECT uuid FROM litebans_bans WHERE banned_by_name = ? " + buildWhereClause(player, status, on, before, after));
            parameters.add(executor);
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("mute")) {
            queries.add("SELECT uuid FROM litebans_mutes WHERE banned_by_name = ? " + buildWhereClause(player, status, on, before, after));
            parameters.add(executor);
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("warning")) {
            queries.add("SELECT uuid FROM litebans_warnings WHERE banned_by_name = ? " + buildWhereClause(player, status, on, before, after));
            parameters.add(executor);
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("kick")) {
            queries.add("SELECT uuid FROM litebans_kicks WHERE banned_by_name = ? " + buildWhereClause(player, status, on, before, after));
            parameters.add(executor);
        }

        baseQuery.append(String.join(" UNION ALL ", queries)).append(") AS t");

        try (PreparedStatement stmt = Database.get().prepareStatement(baseQuery.toString())) {
            int paramIndex = 1;
            for (Object param : parameters) {
                stmt.setString(paramIndex++, (String) param);
            }

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
            }

            if (status != null && !status.isEmpty()) {
                switch (status.toLowerCase()) {
                    case "active":
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        break;
                    case "expired":
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        break;
                }
            }

            if (on != null && !on.isEmpty()) {
                LocalDate date = LocalDate.parse(on);
                long startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                long endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

                stmt.setLong(paramIndex++, startOfDay);
                stmt.setLong(paramIndex++, endOfDay);
            }

            if (before != null && !before.isEmpty()) {
                long beforeTimestamp = LocalDate.parse(before).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                stmt.setLong(paramIndex++, beforeTimestamp);
            }

            if (after != null && !after.isEmpty()) {
                long afterTimestamp = LocalDate.parse(after).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                stmt.setLong(paramIndex++, afterTimestamp);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalRecords = rs.getInt(1);
                    return (int) Math.ceil((double) totalRecords / PAGE_SIZE);
                }
            }
        }
        return 0;
    }

    private String buildQuery(String player, String type, String status, String on, String before, String after, int offset) {
        String baseQuery = "SELECT id, uuid, reason, banned_by_name, ipban, time, until, type FROM (";

        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("ban")) {
            baseQuery += "SELECT id, uuid, reason, banned_by_name, ipban, time, until, 'Ban' AS type FROM litebans_bans WHERE banned_by_name = ?";
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("mute")) {
            baseQuery += " UNION ALL SELECT id, uuid, reason, banned_by_name, ipban, time, until, 'Mute' AS type FROM litebans_mutes WHERE banned_by_name = ?";
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("warning")) {
            baseQuery += " UNION ALL SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, 'Warning' AS type FROM litebans_warnings WHERE banned_by_name = ?";
        }
        if (type == null || type.equalsIgnoreCase("all") || type.equalsIgnoreCase("kick")) {
            baseQuery += " UNION ALL SELECT id, uuid, reason, banned_by_name, ipban, time, NULL AS until, 'Kick' AS type FROM litebans_kicks WHERE banned_by_name = ?";
        }

        baseQuery += ") AS t ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET " + offset;
        return baseQuery;
    }

    private String buildWhereClause(String player, String status, String on, String before, String after) {
        StringBuilder whereClause = new StringBuilder();
    
        if (player != null && !player.isEmpty()) {
            whereClause.append(" AND uuid = ?");
        }
    
        if (status != null && !status.isEmpty()) {
            switch (status.toLowerCase()) {
                case "active":
                    whereClause.append(" AND (removed_by_name IS NULL OR removed_by_name = '')")
                               .append(" AND (until = -1 OR until = 0 OR until > ?)");
                    break;
                case "expired":
                    whereClause.append(" AND (removed_by_name = '#expired' OR (until > 0 AND until < ?))");
                    break;
                case "removed":
                    whereClause.append(" AND (removed_by_name IS NOT NULL AND removed_by_name <> '#expired')");
                    break;
            }
        }
        
        if (on != null && !on.isEmpty()) {
            whereClause.append(" AND time >= ? AND time < ?");
        }

        if (before != null && !before.isEmpty()) {
            whereClause.append(" AND time < ?");
        }
    
        if (after != null && !after.isEmpty()) {
            whereClause.append(" AND time > ?");
        }

        return whereClause.toString();
    }    

    private void fetchAndAddPunishments(String query, StringBuilder punishmentRows, String executor, String player,
                                        String status, String on, String before, String after) throws SQLException {
        
        try (PreparedStatement stmt = Database.get().prepareStatement(query)) {
            int paramIndex = 1;
            
            stmt.setString(paramIndex++, executor);
            stmt.setString(paramIndex++, executor);
            stmt.setString(paramIndex++, executor);
            stmt.setString(paramIndex++, executor);

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
                stmt.setString(paramIndex++, player);
                stmt.setString(paramIndex++, player);
                stmt.setString(paramIndex++, player);
            }
            
            if (status != null && !status.isEmpty()) {
                switch (status.toLowerCase()) {
                    case "active":
                    case "expired":
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        stmt.setLong(paramIndex++, System.currentTimeMillis());
                        break;
                }
            }

            if (on != null && !on.isEmpty()) {
                LocalDate date = LocalDate.parse(on);
                long startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                long endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            
                stmt.setLong(paramIndex++, startOfDay);
                stmt.setLong(paramIndex++, startOfDay);
                stmt.setLong(paramIndex++, startOfDay);
                stmt.setLong(paramIndex++, startOfDay);

                stmt.setLong(paramIndex++, endOfDay);
                stmt.setLong(paramIndex++, endOfDay);
                stmt.setLong(paramIndex++, endOfDay);
                stmt.setLong(paramIndex++, endOfDay);
            }
            
            if (before != null && !before.isEmpty()) {
                long beforeTimestamp = LocalDate.parse(before).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

                stmt.setLong(paramIndex++, beforeTimestamp);
                stmt.setLong(paramIndex++, beforeTimestamp);
                stmt.setLong(paramIndex++, beforeTimestamp);
                stmt.setLong(paramIndex++, beforeTimestamp);
            }
            
            if (after != null && !after.isEmpty()) {
                long afterTimestamp = LocalDate.parse(after).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                stmt.setLong(paramIndex++, afterTimestamp);
                stmt.setLong(paramIndex++, afterTimestamp);
                stmt.setLong(paramIndex++, afterTimestamp);
                stmt.setLong(paramIndex++, afterTimestamp);
            }            

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
        String moderatorName = rs.getString("banned_by_name");
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