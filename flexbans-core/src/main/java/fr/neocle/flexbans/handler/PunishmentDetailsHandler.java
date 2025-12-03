package fr.neocle.flexbans.handler;

import fr.neocle.flexbans.config.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.query.DashboardQueries;
import fr.neocle.flexbans.handler.error.NotFoundError;
import fr.neocle.flexbans.util.DurationCalculator;
import fr.neocle.flexbans.util.HooksUtils;
import fr.neocle.flexbans.util.ResourceLoader;
import fr.neocle.flexbans.util.TextUtils;
import fr.neocle.flexbans.util.player.PlayerHeadImage;
import fr.neocle.flexbans.util.player.UsernameUUIDConverters;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.logging.Logger;

public class PunishmentDetailsHandler extends AbstractHandler {
    private final Logger logger;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils flexbansDatabase;
    private final NotFoundError notFoundError;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> VALID_PUNISHMENT_TYPES = Set.of("bans", "mutes", "warnings", "kicks");

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();

    public PunishmentDetailsHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage,
                                    DatabaseUtils databaseUtils, Logger logger, NotFoundError notFoundError) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.flexbansDatabase = databaseUtils;
        this.logger = logger;
        this.notFoundError = notFoundError;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isValidTarget(target)) return;

        configureResponse(response, baseRequest);

        PunishmentRequest punishmentRequest = parsePunishmentRequest(target, response);
        if (punishmentRequest == null) return;

        String htmlTemplate = loadTemplate(response);
        if (htmlTemplate == null) return;

        PunishmentDetails details = fetchPunishmentDetails(punishmentRequest, response);
        if (details == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            notFoundError.handle(request, response);
            return;
        }

        String pageContent;
        try {
            pageContent = buildPageContent(htmlTemplate, punishmentRequest, details, request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        response.getWriter().write(pageContent);
    }

    private boolean isValidTarget(String target) {
        return target.startsWith("/details/");
    }

    private void configureResponse(HttpServletResponse response, Request baseRequest) {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }

    private PunishmentRequest parsePunishmentRequest(String target, HttpServletResponse response) throws IOException {
        String[] parts = target.split("/");
        if (parts.length < 4) {
            response.getWriter().write("Error: Punishment type and ID are required.");
            return null;
        }

        String punishmentType = parts[2];
        String punishmentId = parts[3];

        if (!isValidPunishmentType(punishmentType)) {
            response.getWriter().write("Error: Invalid punishment type.");
            return null;
        }

        String modifiedPunishmentType = removePlural(punishmentType);
        String tableName = DashboardQueries.getTableName(usingFlexBans, usingLiteBans, punishmentType);

        return new PunishmentRequest(punishmentType, punishmentId, modifiedPunishmentType, tableName);
    }

    private boolean isValidPunishmentType(String type) {
        return VALID_PUNISHMENT_TYPES.contains(type);
    }

    private String removePlural(String punishmentType) {
        return punishmentType.endsWith("s") ?
                punishmentType.substring(0, punishmentType.length() - 1) :
                punishmentType;
    }

    private String loadTemplate(HttpServletResponse response) throws IOException {
        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/punishment_details.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for punishment details page.");
            response.getWriter().write("Error: Unable to load HTML template.");
        }
        return htmlTemplate;
    }

    private PunishmentDetails fetchPunishmentDetails(PunishmentRequest request, HttpServletResponse response) throws IOException {
        String query = DashboardQueries.buildPunishmentDetailsQuery(usingFlexBans, request.tableName);

        try (PreparedStatement stmt = prepareStatement(query)) {
            stmt.setString(1, request.punishmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractPunishmentDetails(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private PreparedStatement prepareStatement(String query) throws SQLException {
        if (usingFlexBans) {
            return flexbansDatabase.prepareStatement(query);
        } else if (usingLiteBans) {
            return Database.get().prepareStatement(query);
        } else {
            logger.severe("No database system is active.");
            throw new SQLException("No database system is active.");
        }
    }

    private PunishmentDetails extractPunishmentDetails(ResultSet rs) throws SQLException {
        String status = null;
        if (usingFlexBans) {
            status = rs.getString("status");
        }

        long removedTimeMillis = rs.getLong("removed_by_date");
        Timestamp removedTime = (removedTimeMillis > 0) ? new Timestamp(removedTimeMillis) : null;

        return new PunishmentDetails(
                rs.getString("uuid"),
                rs.getString("reason"),
                rs.getString("banned_by_uuid"),
                rs.getString("banned_by_name"),
                rs.getString("removed_by_name"),
                rs.getString("removed_by_reason"),
                rs.getLong("time"),
                rs.getLong("until"),
                rs.getString("server_origin"),
                removedTime,
                status
        );
    }

    private String buildPageContent(String htmlTemplate, PunishmentRequest request,
                                    PunishmentDetails details, HttpServletRequest httpRequest) throws Exception {
        PunishmentStatus status = determinePunishmentStatus(details);
        ServerConfig serverConfig = loadServerConfig();
        UIConfig uiConfig = loadUIConfig();

        String executorName = resolveExecutorName(details);
        String playerName = usernameUUIDConverters.UUIDtoUsername(details.playerUUID);

        String date = DATE_FORMAT.format(new Date(details.time));
        String expirationDate = formatExpirationDate(details, status);
        String duration = calculateDuration(details);

        String executorHead = createPlayerHeadSpan(executorName);
        String playerHead = createPlayerHeadSpan(playerName != null ? playerName : "Unknown");
        String rawPlayerHead = playerHeadImage.getPlayerHeadUrl(playerName != null ? playerName : "Unknown", "32");

        String removalReason = buildRemovalReason(status, request.modifiedPunishmentType, details.removedReason);
        String revokeButton = buildRevokeButton(status, uiConfig);
        String identifier = resolveUserIdentifier(httpRequest, playerName);

        return htmlTemplate
                .replace("{{punishment_title}}", buildPunishmentTitle(request, status))
                .replace("{{punishment_page_title}}", TextUtils.capitalize(request.modifiedPunishmentType) + " #" + request.punishmentId)
                .replace("{{punishment_description}}", buildPunishmentDescription(executorName, playerName, duration, date))
                .replace("{{executor}}", executorHead)
                .replace("{{player_name}}", playerHead)
                .replace("{{reason}}", details.reason != null ? details.reason : "N/A")
                .replace("{{execution_date}}", date)
                .replace("{{expiration_date}}", expirationDate)
                .replace("{{duration}}", duration)
                .replace("{{origin_server}}", formatServerOrigin(details.serverOrigin))
                .replace("{{remover_name}}", identifier != null ? identifier : "Unknown")
                .replace("{{removal_reason}}", removalReason)
                .replace("{{revoke_button}}", revokeButton)
                .replace("{{punishment_id}}", request.punishmentId)
                .replace("{{punishment_type}}", request.modifiedPunishmentType)
                .replace("{{server_logo}}", serverConfig.logo)
                .replace("{{favicon}}", rawPlayerHead)
                .replace("{{server_color}}", serverConfig.color)
                .replace("{{server_color_hover}}", serverConfig.colorDarker);
    }

    private PunishmentStatus determinePunishmentStatus(PunishmentDetails details) {
        boolean isExpiredByTime = details.until != -1 && details.until != 0 && details.until < System.currentTimeMillis();
        boolean isManuallyRemoved = false;
        boolean isExplicitlyExpired = false;

        if (usingLiteBans) {
            if (details.removedByName != null) {
                if ("#expired".equals(details.removedByName)) {
                    isExplicitlyExpired = true;
                } else if (details.removedByDate != null &&
                        details.removedByDate.before(new Date())) {
                    isManuallyRemoved = true;
                }
            }
        }

        if (usingFlexBans && details.status != null) {
            if ("expired".equalsIgnoreCase(details.status)) {
                isExplicitlyExpired = true;
                isExpiredByTime = true;
            } else if ("removed".equalsIgnoreCase(details.status)) {
                isManuallyRemoved = true;
            }
        }

        if (isManuallyRemoved) {
            return new PunishmentStatus("Removed", "bg-orange-500 text-white");
        } else if (isExplicitlyExpired || isExpiredByTime) {
            return new PunishmentStatus("Expired", "bg-red-500 text-white");
        } else {
            return new PunishmentStatus("Active", "bg-green-500 text-white");
        }
    }

    private ServerConfig loadServerConfig() {
        return new ServerConfig(
                ConfigManager.getString("server-display.logo"),
                ConfigManager.getString("server-display.color"),
                ConfigManager.getString("server-display.darker-color")
        );
    }

    private UIConfig loadUIConfig() {
        return new UIConfig(
                ConfigManager.getBoolean("discord-oauth.enabled"),
                ConfigManager.getBoolean("password-auth.enabled"),
                ConfigManager.getBoolean("webserver.pages.details.punishment.revoke-button")
        );
    }

    private String resolveExecutorName(PunishmentDetails details) {
        if ("console".equalsIgnoreCase(details.bannedByUUID) && !"console".equalsIgnoreCase(details.bannedByName)) {
            return details.bannedByName;
        } else if ("console".equalsIgnoreCase(details.bannedByName)) {
            return "Console";
        } else {
            return usernameUUIDConverters.UUIDtoUsername(details.bannedByUUID);
        }
    }

    private String formatExpirationDate(PunishmentDetails details, PunishmentStatus status) {
        String baseDate = (details.until == 0 || details.until == -1) ?
                "Never" :
                DATE_FORMAT.format(new Date(details.until));

        if (details.removedByName != null) {
            baseDate += " (removed by " + details.removedByName + ")";
        }

        return baseDate;
    }

    private String calculateDuration(PunishmentDetails details) {
        return (details.until == 0 || details.until == -1) ?
                "Permanent" :
                DurationCalculator.calculateDuration(details.time, details.until);
    }

    private String createPlayerHeadSpan(String playerName) throws Exception {
        return String.format(
                "<span style='display: inline-flex; align-items: center;'>" +
                        "<img src='%s' alt='%s' width='32' height='32' style='margin-right: 8px;'>%s</span>",
                playerHeadImage.getPlayerHeadUrl(playerName, "32"),
                playerName,
                playerName
        );
    }

    private String buildPunishmentTitle(PunishmentRequest request, PunishmentStatus status) {
        String statusLabel = String.format(
                "<span class='px-3 py-1 inline-flex text-xl leading-5 font-semibold rounded-xl %s'>%s</span>",
                status.badgeColorClass,
                status.text
        );
        return TextUtils.capitalize(request.modifiedPunishmentType) + " #" + request.punishmentId + " " + statusLabel;
    }

    private String buildPunishmentDescription(String executorName, String playerName, String duration, String date) {
        return String.format("%s punished %s for %s on %s.", executorName, playerName, duration, date);
    }

    private String formatServerOrigin(String serverOrigin) {
        if (serverOrigin != null && serverOrigin.equals("litebans")) {
            return "Proxy";
        } else if (serverOrigin != null) {
            return serverOrigin;
        } else {
            return "Global";
        }
    }

    private String buildRemovalReason(PunishmentStatus status, String punishmentType, String removedReason) {
        if ("Removed".equals(status.text)) {
            return String.format(
                    "<tr><th class='w-1/3 bg-[#ccc] dark:bg-[#444]'>Un%s Reason</th><td>%s</td></tr>",
                    punishmentType.toLowerCase(),
                    removedReason != null ? removedReason : "N/A"
            );
        }
        return "";
    }

    private String buildRevokeButton(PunishmentStatus status, UIConfig uiConfig) {
        if ("Active".equalsIgnoreCase(status.text) &&
                (uiConfig.oauthEnabled || uiConfig.loginEnabled) &&
                uiConfig.revocationEnabled) {

            return "<div class=\"mt-8 text-center\">\r\n" +
                    "    <button id=\"revoke-button\" class=\"bg-red-500 hover:bg-red-600 text-white py-2 px-6 rounded-lg\">\r\n" +
                    "        <i class=\"fa-solid fa-ban mr-2\"></i> Revoke Punishment\r\n" +
                    "    </button>\r\n" +
                    "</div>";
        }
        return "";
    }

    private String resolveUserIdentifier(HttpServletRequest request, String playerName) {
        String userId = (String) request.getSession().getAttribute("userId");
        String sessionPlayerName = (String) request.getSession().getAttribute("playerName");

        if (userId == null) {
            return sessionPlayerName;
        } else if (playerName == null) {
            return flexbansDatabase.getUserManager().getUsernameFromDiscordId(userId);
        } else {
            return sessionPlayerName;
        }
    }

    private record PunishmentRequest(String punishmentType, String punishmentId, String modifiedPunishmentType,
                                     String tableName) {}

    private record PunishmentDetails(String playerUUID, String reason, String bannedByUUID, String bannedByName,
                                     String removedByName, String removedReason, long time, long until,
                                     String serverOrigin, Timestamp removedByDate, String status) {}

    private record PunishmentStatus(String text, String badgeColorClass) {}

    private record ServerConfig(String logo, String color, String colorDarker) {}

    private record UIConfig(boolean oauthEnabled, boolean loginEnabled, boolean revocationEnabled) {}
}