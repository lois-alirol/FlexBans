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

public class IndexHandler extends AbstractHandler {
    private static final int PAGE_SIZE = 20;
    private final Map<String, Object> config;
    private Logger logger = Logger.getLogger("LitebansWeb");
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
                // Fetch punishment counts
                bansCount = getPunishmentCount("litebans_bans");
                mutesCount = getPunishmentCount("litebans_mutes");
                kicksCount = getPunishmentCount("litebans_kicks");
                warningsCount = getPunishmentCount("litebans_warnings");

                // Calculate total pages
                totalPages = getTotalPages(dbTable);

                // Adjust the current page
                if (page > totalPages && totalPages > 0) {
                    page = totalPages;
                }

                int offset = (page - 1) * PAGE_SIZE;

                // Fetch punishments
                String query = "SELECT * FROM " + dbTable + " ORDER BY time DESC LIMIT " + PAGE_SIZE + " OFFSET " + offset;
                fetchAndAddPunishments(query, punishmentRows, type, false);

            } catch (SQLException e) {
                e.printStackTrace();
                response.getWriter().write("Error: Unable to fetch punishment data.");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server_display_settings");
            String serverName = String.valueOf(serverDisplaySettings.getOrDefault("name", "Example"));
            String serverIcon = String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png"));
            String serverFavicon = String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png"));
            String serverLogo = String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png"));
            String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
            String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker_color", "#207dd2"));
            String serverDescription = String.valueOf(serverDisplaySettings.getOrDefault("description", "ExampleServer: Punishments, view detailed records of every user sanction. Find and review all past bans, mutes, and kicks in one convenient place."));

            @SuppressWarnings("unchecked")
            Map<String, Object> oauth = (Map<String, Object>) config.get("discord_oauth");
            boolean oauthEnabled = Boolean.parseBoolean(String.valueOf(oauth.getOrDefault("enabled", false)));

            @SuppressWarnings("unchecked")
            Map<String, Object> login = (Map<String, Object>) config.get("password_login");
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

    private int getTotalPages(String dbTable) throws SQLException {
        String query = "SELECT COUNT(*) FROM " + dbTable;
        try (PreparedStatement stmt = Database.get().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int totalRecords = rs.getInt(1);
                return (int) Math.ceil((double) totalRecords / PAGE_SIZE);
            }
        }
        return 0;
    }

    private void fetchAndAddPunishments(String query, StringBuilder punishmentRows, String type, boolean isExpired) throws SQLException {
        try (PreparedStatement stmt = Database.get().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                addPunishmentRow(rs, punishmentRows, isExpired, type);
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
    
            // Status Column
            punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                    .append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                    .append(badgeColorClass).append("'>")
                    .append(status)
                    .append("</span>")
                    .append("</td>");
            
            // Player Name Column
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
                    
            // Reason Column
            punishmentRows.append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                    .append(reason)
                    .append("</td>");
            
            // Execution Date Column
            punishmentRows.append("<td class='px-6 py-4'>")
                    .append(executionDate)
                    .append("</td>");
            
            // Expiration Date and Duration Columns (if not a "kick" type)
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