package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.utils.DurationCalculator;
import fr.neocle.flexbans.utils.Player.PlayerHeadImage;
import fr.neocle.flexbans.utils.Player.UsernameUUIDConverters;
import fr.neocle.flexbans.utils.ResourceLoader;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.logging.Logger;

public class IndexHandler extends AbstractHandler {
    private static final int PAGE_SIZE = 20;
    private final Map<String, Object> config;
    private Logger logger = Logger.getLogger("FlexBans");
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DurationCalculator durationCalculator;

    public IndexHandler(Map<String, Object> config, UsernameUUIDConverters usernameUUIDConverters, DurationCalculator durationCalculator, PlayerHeadImage playerHeadImage) {
        this.config = config;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.durationCalculator = durationCalculator;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (target.startsWith("/punishments") || target.startsWith("/index") || target.startsWith("/login")) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            String type = request.getParameter("type");
            if (type == null) {
                type = "bans";
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

            String dbTable = getTableName(type);

            String player = request.getParameter("player");
            if (player != null && (!player.matches("^[0-9a-fA-F]{32}$") || !player.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
                player = usernameUUIDConverters.usernameToUUID(player);
            }

            String executor = request.getParameter("executor");
            if (executor != null && executor.equalsIgnoreCase("[console]")) {
                executor = "Console";
            } else if (executor != null && (executor.matches("^[0-9a-fA-F]{32}$") || executor.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
                executor = usernameUUIDConverters.UUIDtoUsername(executor);
            }

            String status = request.getParameter("status");
            String on = request.getParameter("on");
            String before = request.getParameter("before");
            String after = request.getParameter("after");

            String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/index.html");
            if (htmlTemplate == null) {
                logger.warning("Unable to load HTML template for index page.");
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            StringBuilder punishmentRows = new StringBuilder();
            int totalPages = 0;
            int bansCount = 0;
            int mutesCount = 0;
            int kicksCount = 0;
            int warningsCount = 0;

            try {
                bansCount = getPunishmentCount("litebans_bans");
                mutesCount = getPunishmentCount("litebans_mutes");
                kicksCount = getPunishmentCount("litebans_kicks");
                warningsCount = getPunishmentCount("litebans_warnings");

                totalPages = getTotalPages(dbTable, player, executor, status, on, before, after);

                if (page > totalPages && totalPages > 0) {
                    page = totalPages;
                }

                int offset = (page - 1) * PAGE_SIZE;

                String query = buildQuery(dbTable, player, executor, status, on, before, after, offset);
                fetchAndAddPunishments(query, punishmentRows, type, false, player, executor, status, on, before, after);

            } catch (SQLException e) {
                e.printStackTrace();
                response.getWriter().write("Error: Unable to fetch punishment data.");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server-display");
            String serverName = String.valueOf(serverDisplaySettings.getOrDefault("name", "Example"));
            String serverIcon = String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png"));
            String serverFavicon = String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png"));
            String serverLogo = String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png"));
            String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
            String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker-color", "#207dd2"));
            String serverDescription = String.valueOf(serverDisplaySettings.getOrDefault("description", "ExampleServer: Punishments, view detailed records of every user sanction. Find and review all past bans, mutes, and kicks in one convenient place."));

            @SuppressWarnings("unchecked")
            Map<String, Object> oauth = (Map<String, Object>) config.get("discord-oauth");
            boolean oauthEnabled = Boolean.parseBoolean(String.valueOf(oauth.getOrDefault("enabled", false)));

            @SuppressWarnings("unchecked")
            Map<String, Object> login = (Map<String, Object>) config.get("password-auth");
            boolean loginEnabled = Boolean.parseBoolean(String.valueOf(login.getOrDefault("enabled", false)));

            String newPunishmentButton = "";
            if (oauthEnabled || loginEnabled) {
                newPunishmentButton = "<hr class=\"border-gray-600 my-4 w-3/4 mx-auto\">" +
                        "<div class=\"flex w-full max-w-md space-x-2 justify-center\">" +
                        "<a href=\"/new-punishment\" class=\"nav-item block py-2.5 px-4 mx-4 rounded-lg text-white bg-[" + serverColor + "] hover:bg-[" + serverColorDarker + "] relative\">" +
                        "<i class=\"fa-solid fa-gavel\"></i> <span class=\"ml-4 text-lg\">New Punishment</span>" +
                        "</a>" +
                        "</div>";
            }

            String pageContent = htmlTemplate
                    .replace("{{punishment_rows}}", punishmentRows.toString())
                    .replace("{{punishment_type}}", type)
                    .replace("{{punishment_type_capitalized}}", capitalize(type))
                    .replace("{{server_name}}", serverName)
                    .replace("{{server_description}}", serverDescription)
                    .replace("{{server_icon}}", serverIcon)
                    .replace("{{server_favicon}}", serverFavicon)
                    .replace("{{server_color}}", serverColor)
                    .replace("{{server_color_hover}}", serverColorDarker)
                    .replace("{{server_logo}}", serverLogo)
                    .replace("{{new_punishment_button}}", newPunishmentButton)
                    .replace("{{current_page}}", String.valueOf(page))
                    .replace("{{total_pages}}", String.valueOf(totalPages))
                    .replace("{{bans_count}}", String.valueOf(bansCount))
                    .replace("{{mutes_count}}", String.valueOf(mutesCount))
                    .replace("{{kicks_count}}", String.valueOf(kicksCount))
                    .replace("{{warnings_count}}", String.valueOf(warningsCount));

            response.getWriter().write(pageContent);
        }
    }

    private int getPunishmentCount(String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = Database.get().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private int getTotalPages(String dbTable, String player, String executor,
                              String status, String on, String before, String after) throws SQLException {

        String query = "SELECT COUNT(*) FROM " + dbTable + " WHERE 1=1 " + buildWhereClause(player, executor, status, on, before, after);

        try (PreparedStatement stmt = Database.get().prepareStatement(query)) {
            int paramIndex = 1;

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
            }
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
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

    private String buildQuery(String dbTable, String player, String executor, String status, String on, String before, String after, int offset) {
        String query = "SELECT * FROM " + dbTable + " WHERE 1=1 " + buildWhereClause(player, executor, status, on, before, after) +
                " ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET " + offset;
        return query;
    }

    private String buildWhereClause(String player, String executor, String status, String on, String before, String after) {
        StringBuilder whereClause = new StringBuilder();

        if (player != null && !player.isEmpty()) {
            whereClause.append(" AND uuid = ?");
        }

        if (executor != null && !executor.isEmpty()) {
            whereClause.append(" AND banned_by_name = ?");
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

    private void fetchAndAddPunishments(String query, StringBuilder punishmentRows, String type,
                                        boolean isExpired, String player, String executor,
                                        String status, String on, String before, String after) throws SQLException {

        try (PreparedStatement stmt = Database.get().prepareStatement(query)) {
            int paramIndex = 1;

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
            }
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
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
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows, isExpired, type);
                }
            }
        }
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows, boolean isExpired, String type) throws SQLException {
        String uuid = rs.getString("uuid");
        String playerName = usernameUUIDConverters.UUIDtoUsername(uuid);
        String executor = rs.getString("banned_by_name");
        String reason = rs.getString("reason");

        int punishmentID = rs.getInt("id");
        long time = rs.getLong("time");
        long until = rs.getLong("until");
        String removedByName = "";

        if (!type.equals("kicks")) {
            removedByName = rs.getString("removed_by_name");
        } else {
            removedByName = "null";
        }

        Timestamp removedByDate = null;

        boolean isExpiredByTime = false;
        boolean isManuallyRemoved = false;
        boolean isExplicitlyExpired = false;

        try {
            removedByDate = rs.getTimestamp("removed_by_date");
        } catch (SQLException e) {
        }

        if (until != -1 && until != 0 && until < System.currentTimeMillis()) {
            isExpiredByTime = true;
        }

        if (removedByName != null) {
            if ("#expired".equals(removedByName)) {
                isExplicitlyExpired = true;
            } else if (removedByDate != null && removedByDate.before(new java.util.Date())) {
                isManuallyRemoved = true;
            }
        }

        String status;
        String badgeColorClass;

        if (isManuallyRemoved) {
            status = "Removed";
            badgeColorClass = "bg-orange-500 text-white";
        } else if (isExplicitlyExpired || isExpiredByTime) {
            status = "Expired";
            badgeColorClass = "bg-red-500 text-white";
        } else {
            status = "Active";
            badgeColorClass = "bg-green-500 text-white";
        }

        String executionDate = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(time));
        String expirationDate = (until == -1 || until == 0) ? "Never" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(until));

        String duration = durationCalculator.calculateDuration(time, until);

        String rowClass = isExpired
                ? "bg-light-expired-bans dark:bg-dark-expired-bans text-light-text dark:text-dark-text"
                : "bg-light-active-bans dark:bg-dark-active-bans text-light-text dark:text-dark-text";

        punishmentRows.append("<tr class='cursor-pointer ").append(rowClass).append("' onclick=\"window.location.href='/details/").append(uncapitalize(type)).append("/").append(punishmentID).append("';\">");

        punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                .append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                .append(badgeColorClass).append("'>")
                .append(status)
                .append("</span>")
                .append("</td>");

        try {
            punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                    .append("<img src='").append(playerHeadImage.getPlayerHeadUrl(playerName, "32")).append("' alt='Player Head' class='inline-block'> ")
                    .append("<a href='/player/").append(playerName).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                    .append(playerName).append("</a>")
                    .append("</td>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                    .append("<img src='").append(playerHeadImage.getPlayerHeadUrl(executor, "32")).append("' alt='Executor Head' class='inline-block'> ")
                    .append("<a href='/moderator/").append(executor).append("' onclick='event.stopPropagation();' class='hover:underline'>")
                    .append(executor).append("</a>")
                    .append("</td>");
        } catch (Exception e) {
            e.printStackTrace();
        }

        punishmentRows.append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                .append(reason)
                .append("</td>");

        punishmentRows.append("<td class='px-6 py-4'>")
                .append(executionDate)
                .append("</td>");

        if (!type.equals("kicks")) {
            punishmentRows.append("<td class='px-6 py-4'>")
                    .append(expirationDate)
                    .append("</td>")
                    .append("<td class='px-6 py-4'>")
                    .append(duration)
                    .append("</td>");
        }

        punishmentRows.append("</tr>");
    }

    private String getTableName(String type) {
        switch (type) {
            case "warnings":
                return "litebans_warnings";
            case "mutes":
                return "litebans_mutes";
            case "kicks":
                return "litebans_kicks";
            case "bans":
            default:
                return "litebans_bans";
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

    public void updateConfig(Map<String, Object> newConfig) {
        this.config.clear();
        this.config.putAll(newConfig);
    }
}