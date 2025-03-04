package fr.neocle.litebansweb.handlers;

import fr.neocle.litebansweb.utils.DatabaseUtils;
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

public class PunishmentDetailsHandler extends AbstractHandler {
    private final Logger logger = Logger.getLogger("PunishmentDetailsHandler");
    private final Map<String, Object> config;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final DurationCalculator durationCalculator;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils databaseUtils;

    public PunishmentDetailsHandler(Map<String, Object> config, UsernameUUIDConverters usernameUUIDConverters, DurationCalculator durationCalculator, PlayerHeadImage playerHeadImage, DatabaseUtils databaseUtils) {
        this.config = config;
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.durationCalculator = durationCalculator;
        this.playerHeadImage = playerHeadImage;
        this.databaseUtils = databaseUtils;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!target.startsWith("/details/")) {
            return;
        }
    
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    
        String[] parts = target.split("/");
        if (parts.length < 4) {
            response.getWriter().write("Error: Punishment type and ID are required.");
            return;
        }
    
        String punishmentType = parts[2];
        String punishmentId = parts[3];
    
        if (!isValidPunishmentType(punishmentType)) {
            response.getWriter().write("Error: Invalid punishment type.");
            return;
        }
    
        String tableName = "litebans_" + punishmentType;
        
        String modifiedPunishmentType = punishmentType;
            if (modifiedPunishmentType.endsWith("s")) {
                modifiedPunishmentType = modifiedPunishmentType.substring(0, modifiedPunishmentType.length() - 1);
            }

        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/punishment_details.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for punishment details page.");
            response.getWriter().write("Error: Unable to load HTML template.");
            return;
        }
    
        try (PreparedStatement stmt = Database.get().prepareStatement(
                "SELECT uuid, ip, reason, banned_by_uuid, banned_by_name, removed_by_uuid, removed_by_name, removed_by_reason, removed_by_date, " +
                        "time, until, server_origin, active " +
                        "FROM " + tableName + " WHERE id = ?")) {
            stmt.setString(1, punishmentId);
    
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String playerUUID = rs.getString("uuid");
                    String reason = rs.getString("reason");
                    String bannedByUUID = rs.getString("banned_by_uuid");
                    String bannedByName = rs.getString("banned_by_name");
                    String removedByName = rs.getString("removed_by_name");
                    String removedReason = rs.getString("removed_by_reason");
                    long time = rs.getLong("time");
                    long until = rs.getLong("until");
                    String serverOrigin = rs.getString("server_origin");

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
                    String statusLabel;
                
                    if (isManuallyRemoved) {
                        status = "Removed";
                        statusLabel = "<span class='px-3 py-1 inline-flex text-xl leading-5 font-semibold rounded-xl bg-orange-500 text-white'>Removed</span>";
                    } else if (isExplicitlyExpired || isExpiredByTime) {
                        status = "Expired";
                        statusLabel = "<span class='px-3 py-1 inline-flex text-xl leading-5 font-semibold rounded-xl bg-red-500 text-white'>Expired</span>";
                    } else {
                        status = "Active";
                        statusLabel = "<span class='px-3 py-1 inline-flex text-xl leading-5 font-semibold rounded-xl bg-green-500 text-white'>Active</span>";
                    }

                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(time));
                    String expirationDate = (until == 0 || until == -1) ? "Never" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(until));
    
                    if (removedByName != null) {
                        expirationDate += " (removed by " + removedByName + ")";
                    }
    
                    String duration = (until == 0 || until == -1)
                            ? "Permanent"
                            : durationCalculator.calculateDuration(time, until);
    
                    String executorName = "Console".equalsIgnoreCase(bannedByName) 
                        ? "Console" 
                        : usernameUUIDConverters.UUIDtoUsername(bannedByUUID);
                                                        
                    String playerName = usernameUUIDConverters.UUIDtoUsername(playerUUID);

                    String executorHead = "<span style='display: inline-flex; align-items: center;'>"
                        + "<img src='" + playerHeadImage.getPlayerHeadUrl(executorName, "32") + "' alt='" + executorName + "' width='32' height='32' style='margin-right: 8px;'>"
                        + executorName
                        + "</span>";

                    String rawPlayerHead = playerHeadImage.getPlayerHeadUrl(playerName != null ? playerName : "Unknown", "32");
                    
                    String playerHead = "<span style='display: inline-flex; align-items: center;'>"
                        + "<img src='" + playerHeadImage.getPlayerHeadUrl(playerName != null ? playerName : "Unknown", "32") + "' alt='" + (playerName != null ? playerName : "Unknown") + "' width='32' height='32' style='margin-right: 8px;'>"
                        + (playerName != null ? playerName : "Unknown")
                        + "</span>";
                    
                    String removalReason = "";
                    if ("Removed".equals(status)) {
                        removalReason = "<tr><th class='w-1/3 bg-[#ccc] dark:bg-[#444]'>Un" + modifiedPunishmentType.toLowerCase() + " Reason</th><td>" + removedReason + "</td></tr>";
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server-display");
                    String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
                    String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker-color", "#207dd2"));
                    String serverIcon = String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png"));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> oauth = (Map<String, Object>) config.get("discord-oauth");
                    boolean oauthEnabled = Boolean.parseBoolean(String.valueOf(oauth.getOrDefault("enabled", false)));

                    @SuppressWarnings("unchecked")
                    Map<String, Object> login = (Map<String, Object>) config.get("password-auth");
                    boolean loginEnabled = Boolean.parseBoolean(String.valueOf(login.getOrDefault("enabled", false))); 

                    String revokeButton = "";
                    if ("Active".equals(status) && (oauthEnabled || loginEnabled)) {
                        revokeButton = "<div class=\"mt-8 text-center\">\r\n" +
                                       "    <button id=\"revoke-button\" class=\"bg-red-500 hover:bg-red-600 text-white py-2 px-6 rounded-lg\">\r\n" +
                                       "        <i class=\"fa-solid fa-ban mr-2\"></i> Revoke Punishment\r\n" +
                                       "    </button>\r\n" +
                                       "</div>";
                    }

                    String userId = (String) request.getSession().getAttribute("userId");
                    String identifier = "None";
                    if (userId == null) {
                        identifier = (String) request.getSession().getAttribute("playerName");
                    } else if (playerName == null) {
                        identifier = databaseUtils.getUsernameFromDiscordId(userId);
                    }

                    htmlTemplate = htmlTemplate
                        .replace("{{punishment_title}}", capitalizeFirstLetter(modifiedPunishmentType) + " #" + punishmentId + " " + statusLabel)
                        .replace("{{punishment_page_title}}", capitalizeFirstLetter(modifiedPunishmentType) + " #" + punishmentId)
                        .replace("{{punishment_description}}", executorName + " punished " + playerName + " for " + duration + " on " + date + ".")
                        .replace("{{executor}}", executorHead)
                        .replace("{{player_name}}", playerHead)
                        .replace("{{reason}}", reason != null ? reason : "N/A")
                        .replace("{{execution_date}}", date)
                        .replace("{{expiration_date}}", expirationDate)
                        .replace("{{duration}}", duration)
                        .replace("{{origin_server}}", serverOrigin != null && serverOrigin.equals("litebans") ? "Proxy" : (serverOrigin != null ? serverOrigin : "Global")                        )
                        .replace("{{remover_name}}", identifier != null ? identifier : "Unknown")
                        .replace("{{removal_reason}}", removalReason != null ? removalReason : "N/A")
                        .replace("{{revoke_button}}", revokeButton)
                        .replace("{{punishment_id}}", punishmentId)
                        .replace("{{punishment_type}}", modifiedPunishmentType)
                        .replace("{{server_icon}}", serverIcon)
                        .replace("{{favicon}}", rawPlayerHead)
                        .replace("{{server_color}}", serverColor)
                        .replace("{{server_color_hover}}", serverColorDarker);
                 
                } else {
                    response.getWriter().write("Error: Punishment details not found.");
                    return;
                }
            } catch (Exception e) {
                    e.printStackTrace();
                }
        } catch (SQLException e) {
            logger.severe("SQL Exception: " + e.getMessage());
            response.getWriter().write("Error: Unable to fetch punishment details.");
            return;
        }
    
        response.getWriter().write(htmlTemplate);
    }
    
    private String capitalizeFirstLetter(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private boolean isValidPunishmentType(String type) {
        return "bans".equals(type) ||
               "mutes".equals(type) ||
               "warnings".equals(type) ||
               "kicks".equals(type);
    }
}
