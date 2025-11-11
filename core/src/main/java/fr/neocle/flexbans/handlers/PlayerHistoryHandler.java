package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.queries.DashboardQueries;
import fr.neocle.flexbans.handlers.errors.NotFoundError;
import fr.neocle.flexbans.utils.DurationCalculator;
import fr.neocle.flexbans.utils.HooksUtils;
import fr.neocle.flexbans.utils.ResourceLoader;
import fr.neocle.flexbans.utils.TextUtils;
import fr.neocle.flexbans.utils.player.PlayerHeadImage;
import fr.neocle.flexbans.utils.player.UsernameUUIDConverters;
import litebans.api.Database;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

public class PlayerHistoryHandler extends AbstractHandler {
    private final Logger logger;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils flexbansDatabase;
    private final NotFoundError notFoundError;

    private static final int PAGE_SIZE = ConfigManager.getConfigInt("webserver.pages.details.player.max-per-page");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> TYPE_COLOR_CLASSES = Map.of(
            "IP-Ban", "flex items-center justify-center bg-red-800 text-white",
            "Ban", "flex items-center justify-center bg-red-600 text-white",
            "IP-Mute", "flex items-center justify-center bg-orange-500 text-white",
            "Mute", "flex items-center justify-center bg-orange-400 text-white",
            "Warning", "flex items-center justify-center bg-yellow-400 text-black",
            "Kick", "flex items-center justify-center bg-yellow-400 text-black"
    );

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();
    private int totalRecords;

    public PlayerHistoryHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage,
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

        PlayerRequest playerRequest = parsePlayerRequest(target, request, response);
        if (playerRequest == null) return;

        String htmlTemplate = loadTemplate(response);
        if (htmlTemplate == null) return;

        PlayerData data = fetchPlayerData(playerRequest);
        if (data == null) {
            notFoundError.handle(request, response);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String pageContent = buildPageContent(htmlTemplate, playerRequest, data);
        if (pageContent.contains("Error")) {
            notFoundError.handle(request, response);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.getWriter().write(pageContent);
    }

    private boolean isValidTarget(String target) {
        return target.startsWith("/player/");
    }

    private void configureResponse(HttpServletResponse response, Request baseRequest) {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }

    private PlayerRequest parsePlayerRequest(String target, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String playerIdentifier = extractPlayerIdentifier(target);
        if (playerIdentifier.isEmpty()) {
            response.getWriter().write("Error: Player UUID or Username is required.");
            return null;
        }

        String uuid = resolvePlayerUUID(playerIdentifier, response);
        if (uuid == null) return null;

        int page = parsePageParameter(request.getParameter("page"));
        String executor = normalizeExecutor(request.getParameter("executor"));

        return new PlayerRequest(playerIdentifier, uuid, page, executor);
    }

    private String extractPlayerIdentifier(String target) {
        return target.replace("/player/", "").trim();
    }

    private String resolvePlayerUUID(String playerIdentifier, HttpServletResponse response) throws IOException {
        if (isUUID(playerIdentifier)) {
            return playerIdentifier;
        }

        String uuid = usernameUUIDConverters.usernameToUUID(playerIdentifier);
        if (uuid == null) {
            response.getWriter().write("Error: Could not find UUID for the given username.");
            return null;
        }
        return uuid;
    }

    private boolean isUUID(String identifier) {
        return identifier.length() == 36;
    }

    private int parsePageParameter(String pageParam) {
        if (pageParam == null) return 1;
        try {
            return Integer.parseInt(pageParam);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String normalizeExecutor(String executor) {
        if (executor == null) return null;
        if (isUUIDPattern(executor)) {
            return usernameUUIDConverters.UUIDtoUsername(executor);
        }
        return executor;
    }

    private boolean isUUIDPattern(String identifier) {
        return identifier != null && (
                identifier.matches("^[0-9a-fA-F]{32}$") ||
                        identifier.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        );
    }

    private String loadTemplate(HttpServletResponse response) throws IOException {
        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/player_history.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for player history page.");
            response.getWriter().write("Error: Unable to load HTML template.");
        }
        return htmlTemplate;
    }

    private PlayerData fetchPlayerData(PlayerRequest request) {
        try {
            int totalPages = getTotalPages(request.uuid, request.executor);

            int adjustedPage = request.page;
            if (adjustedPage > totalPages && totalPages > 0) {
                adjustedPage = totalPages;
            }

            int offset = (adjustedPage - 1) * PAGE_SIZE;
            StringBuilder punishmentRows = new StringBuilder();

            if (totalPages > 0) {
                fetchAndAddPunishments(punishmentRows, request.uuid, request.executor, offset);
            }

            return new PlayerData(punishmentRows, Math.max(totalPages, 1), adjustedPage, totalRecords);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildPageContent(String htmlTemplate, PlayerRequest request, PlayerData data) {
        ServerConfig serverConfig = loadServerConfig();
        UIComponents components = buildUIComponents(data, request);

        String playerDescription = String.format(
                "Check all the punishments of %s here. So far, they received a total of %d punishments.",
                TextUtils.capitalize(request.playerIdentifier),
                data.totalRecords
        );

        boolean hasResults = data.punishmentRows.length() > 0;
        String tableContent = hasResults ? buildTableHtml(data.punishmentRows.toString()) : "";
        String emptyStateContent = hasResults ? "" : buildEmptyStateSection(request.playerIdentifier);
        String paginationContent = hasResults && data.totalPages > 1 ? buildPaginationHtml(data.currentPage, data.totalPages) : "";

        try {
            return htmlTemplate
                    .replace("{{back_to_index_button}}", components.backToIndexButton)
                    .replace("{{search_moderator_section}}", components.searchModeratorSection)
                    .replace("{{table_content}}", tableContent)
                    .replace("{{empty_state_content}}", emptyStateContent)
                    .replace("{{pagination_content}}", paginationContent)
                    .replace("{{player_name}}", TextUtils.capitalize(request.playerIdentifier))
                    .replace("{{current_page}}", String.valueOf(data.currentPage))
                    .replace("{{total_pages}}", String.valueOf(data.totalPages))
                    .replace("{{favicon}}", playerHeadImage.getPlayerHeadUrl(request.playerIdentifier, "32"))
                    .replace("{{player_description}}", playerDescription)
                    .replace("{{player_icon}}", playerHeadImage.getPlayerHeadUrl(request.playerIdentifier, "512"))
                    .replace("{{server_color}}", serverConfig.color)
                    .replace("{{server_color_hover}}", serverConfig.colorDarker);
        } catch (Exception e) {
            return "Error: Unable to generate player history page.";
        }
    }

    private UIComponents buildUIComponents(PlayerData data, PlayerRequest request) {
        ServerConfig serverConfig = loadServerConfig();

        String backToIndexButton = createBackToIndexButton(serverConfig.color, serverConfig.colorDarker);

        String searchModeratorSection = data.totalRecords > 0
                ? createSearchModeratorSection()
                : "";

        return new UIComponents(backToIndexButton, searchModeratorSection);
    }

    private String createBackToIndexButton(String serverColor, String serverColorDarker) {
        return String.format("""
                <a href="/index"
                   class="absolute top-0 left-0 mt-2 ml-2 bg-[%s] text-white py-2 px-4 rounded-lg hover:bg-[%s]">
                    <i class="fa-solid fa-arrow-left mr-2"></i> Back to Index
                </a>
                """, serverColor, serverColorDarker);
    }

    private String createSearchModeratorSection() {
        return """
                <div class="search mb-2 mt-5 flex items-center">
                    <div class="relative w-full">
                        <input type="text" id="moderatorSearchInput" placeholder="Search Moderator..."
                               class="w-full p-3.5 pl-12 bg-[#f0f0f0cc] dark:bg-[#3b3b3bcc] rounded-lg shadow-custom focus:outline-none">
                        <i class="fa-solid fa-gavel absolute left-4 top-1/2 transform -translate-y-1/2 text-[#333333] dark:text-[#e0e0e0]"></i>
                    </div>
                </div>
                """;
    }

    private String buildTableHtml(String punishmentRows) {
        return String.format("""
                <div>
                    <table id="punishmentTable"
                           class="table-auto w-full bg-[#ffffffe6] dark:bg-[#2c2c2ce6] text-[#333333] dark:text-[#e0e0e0] rounded-lg overflow-hidden shadow-custom">
                        <thead>
                        <tr class="bg-[#ccc] dark:bg-[#444] text-left text-slate-800 dark:text-slate-300">
                            <th class="px-4 py-2">Type</th>
                            <th class="px-4 py-2">Player</th>
                            <th class="px-4 py-2">Executor</th>
                            <th class="px-4 py-2">Reason</th>
                            <th class="px-4 py-2">Execution Date</th>
                            <th class="px-4 py-2">Expiration Date</th>
                            <th class="px-4 py-2">Duration</th>
                            <th class="px-4 py-2">Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        %s
                        </tbody>
                    </table>
                </div>
                """, punishmentRows);
    }

    private String buildEmptyStateSection(String playerName) {
        return String.format("""
                <div class="flex flex-col items-center justify-center py-16 px-4">
                    <div class="empty-state-container text-center">
                        <i class="fas fa-user-check empty-state-icon text-8xl text-gray-400 dark:text-gray-600 mb-6"></i>
                        <h3 class="text-2xl font-bold text-gray-600 dark:text-gray-400 mb-3">No Punishments Found</h3>
                        <p class="text-lg text-gray-500 dark:text-gray-500 mb-4">%s has no punishments in the database.</p>
                        <div class="text-sm text-gray-400 dark:text-gray-600">
                            <i class="fas fa-info-circle mr-2"></i>
                            This player has a clean record
                        </div>
                    </div>
                </div>
                """, TextUtils.capitalize(playerName));
    }

    private String buildPaginationHtml(int currentPage, int totalPages) {
        return String.format("""
                <div class="flex justify-center mt-8 gap-4">
                    <a href="#" id="prevPage"
                       class="bg-[#c1c1c1] dark:bg-[#444] text-white py-2 px-4 rounded-md hover:bg-gray-600 disabled:bg-gray-500">
                        <i class="fa-solid fa-angles-left"></i>
                    </a>
                    <input type="number" id="pageInput" min="1" max="%d" value="%d"
                           class="text-center w-16 bg-[#c1c1c1] dark:bg-[#444] text-black dark:text-white p-2 rounded-md">
                    <a href="#" id="nextPage"
                       class="bg-[#c1c1c1] dark:bg-[#444] text-white py-2 px-4 rounded-md hover:bg-gray-600 disabled:bg-gray-500">
                        <i class="fa-solid fa-angles-right"></i>
                    </a>
                </div>
                """, totalPages, currentPage);
    }

    private ServerConfig loadServerConfig() {
        return new ServerConfig(
                (String) ConfigManager.getConfigValue("server-display.color"),
                (String) ConfigManager.getConfigValue("server-display.darker-color")
        );
    }

    private int getTotalPages(String player, String executor) throws SQLException {
        String query = DashboardQueries.buildPlayerCountQuery(usingFlexBans, player, executor);

        try (PreparedStatement stmt = prepareStatement(query)) {
            setPlayerParameters(stmt, player, executor);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalRecords = rs.getInt(1);
                    return (int) Math.ceil((double) totalRecords / PAGE_SIZE);
                }
            }
        }
        return 0;
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

    private void setPlayerParameters(PreparedStatement stmt, String player, String executor) throws SQLException {
        int paramIndex = 1;

        for (int i = 0; i < 4; i++) {
            stmt.setString(paramIndex++, player);
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }
        }
    }

    private void fetchAndAddPunishments(StringBuilder punishmentRows, String player, String executor, int offset) throws SQLException {
        String query = DashboardQueries.buildPlayerPunishmentsQuery(usingFlexBans, player, executor, PAGE_SIZE, offset);

        try (PreparedStatement stmt = prepareStatement(query)) {
            setPlayerParameters(stmt, player, executor);

            stmt.setInt(stmt.getParameterMetaData().getParameterCount() - 1, PAGE_SIZE);
            stmt.setInt(stmt.getParameterMetaData().getParameterCount(), offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows);
                }
            }
        }
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows) throws SQLException {
        PunishmentRowData rowData = extractPunishmentRowData(rs);
        PunishmentStatus status = determinePunishmentStatus(rowData);

        String playerName = usernameUUIDConverters.UUIDtoUsername(rowData.playerUUID);
        String executor = rowData.executorName != null ? rowData.executorName : "Console";
        String typeDisplay = formatPunishmentType(rowData.type, rowData.isIpBan);
        String typeColorClass = getTypeColorClass(typeDisplay);

        buildPunishmentRowHtml(punishmentRows, rowData, status, playerName, executor, typeDisplay, typeColorClass);
    }

    private PunishmentRowData extractPunishmentRowData(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();

        String status = null;
        if (usingFlexBans && hasColumn(metaData, "status")) {
            status = rs.getString("status");
        }

        return new PunishmentRowData(
                rs.getString("type").toLowerCase(),
                rs.getString("uuid"),
                rs.getString("reason"),
                rs.getString("banned_by_name"),
                rs.getLong("time"),
                rs.getLong("until"),
                rs.getInt("ipban") == 1,
                rs.getInt("id"),
                extractRemovedByData(rs, metaData),
                status
        );
    }

    private RemovedByData extractRemovedByData(ResultSet rs, ResultSetMetaData metaData) throws SQLException {
        boolean hasRemovedByName = hasColumn(metaData, "removed_by_name");
        boolean hasRemovedByDate = hasColumn(metaData, "removed_by_date");

        String removedByName = null;
        Timestamp removedByDate = null;

        if (hasRemovedByName) {
            removedByName = rs.getString("removed_by_name");
        }
        if (hasRemovedByDate) {
            removedByDate = rs.getTimestamp("removed_by_date");
        }

        return new RemovedByData(removedByName, removedByDate);
    }

    private boolean hasColumn(ResultSetMetaData metaData, String columnName) throws SQLException {
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (metaData.getColumnName(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private String formatPunishmentType(String type, boolean isIpBan) {
        if (isIpBan) {
            return type.equals("ban") ? "IP-Ban" : "IP-Mute";
        }

        return switch (type) {
            case "ban" -> "Ban";
            case "mute" -> "Mute";
            case "warning" -> "Warning";
            case "kick" -> "Kick";
            default -> TextUtils.capitalize(type);
        };
    }

    private PunishmentStatus determinePunishmentStatus(PunishmentRowData rowData) {
        if (rowData.type.equals("kick")) {
            return new PunishmentStatus(" / ", "flex items-center justify-center bg-gray-500 text-white");
        }

        boolean isExpiredByTime = rowData.until != -1 && rowData.until != 0 && rowData.until < System.currentTimeMillis();
        boolean isManuallyRemoved = false;
        boolean isExplicitlyExpired = false;

        // Check LiteBans removed_by fields
        if (usingLiteBans) {
            if (rowData.removedByData.removedByName != null) {
                if ("#expired".equals(rowData.removedByData.removedByName)) {
                    isExplicitlyExpired = true;
                } else if (rowData.removedByData.removedByDate != null &&
                        rowData.removedByData.removedByDate.before(new java.util.Date())) {
                    isManuallyRemoved = true;
                }
            }
        }

        // Check FlexBans status field
        if (usingFlexBans && rowData.status != null) {
            if ("expired".equalsIgnoreCase(rowData.status)) {
                isExplicitlyExpired = true;
                isExpiredByTime = true;
            } else if ("removed".equalsIgnoreCase(rowData.status)) {
                isManuallyRemoved = true;
            }
        }

        // Return status based on checks
        if (isManuallyRemoved) {
            return new PunishmentStatus("Removed", "flex items-center justify-center bg-orange-500 text-white");
        } else if (isExplicitlyExpired || isExpiredByTime) {
            return new PunishmentStatus("Expired", "flex items-center justify-center bg-red-500 text-white");
        } else {
            return new PunishmentStatus("Active", "flex items-center justify-center bg-green-500 text-white");
        }
    }

    private void buildPunishmentRowHtml(StringBuilder punishmentRows, PunishmentRowData rowData,
                                        PunishmentStatus status, String playerName, String executor,
                                        String typeDisplay, String typeColorClass) {
        String date = DATE_FORMAT.format(new Date(rowData.time));
        String expirationDate = formatExpirationDate(rowData.until);
        String duration = DurationCalculator.calculateDuration(rowData.time, rowData.until);

        try {
            punishmentRows.append("<tr onclick=\"window.location.href='/details/")
                    .append(TextUtils.unCapitalize(typeDisplay)).append("s/").append(rowData.punishmentID).append("';\">")

                    .append("<td><span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                    .append(typeColorClass).append("'>").append(typeDisplay).append("</span></td>")

                    .append("<td><img src='").append(playerHeadImage.getPlayerHeadUrl(playerName, "32"))
                    .append("' alt='Player Head' class='inline-block'> ")
                    .append("<a href='/player/").append(playerName != null ? playerName : rowData.playerUUID)
                    .append("' onclick='event.stopPropagation();' class='hover:underline'>")
                    .append(playerName != null ? playerName : rowData.playerUUID).append("</a></td>")

                    .append("<td><img src='").append(playerHeadImage.getPlayerHeadUrl(executor, "32"))
                    .append("' alt='Executor Head' class='inline-block'> ")
                    .append("<a href='/moderator/").append(executor)
                    .append("' onclick='event.stopPropagation();' class='hover:underline'>")
                    .append(executor).append("</a></td>")

                    .append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                    .append(rowData.reason).append("</td>")

                    .append("<td>").append(date).append("</td>")

                    .append("<td>").append(expirationDate).append("</td>")

                    .append("<td>").append(duration).append("</td>")

                    .append("<td><span class='flex items-center justify-center px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                    .append(status.badgeColorClass).append("'>").append(status.text).append("</span></td>")

                    .append("</tr>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatExpirationDate(long until) {
        return (until == 0 || until == -1) ? "Never" : DATE_FORMAT.format(new Date(until));
    }

    private String getTypeColorClass(String type) {
        return TYPE_COLOR_CLASSES.getOrDefault(type, "flex items-center justify-center bg-gray-500 text-white");
    }

    private record PlayerRequest(String playerIdentifier, String uuid, int page, String executor) {
    }

    private record PlayerData(StringBuilder punishmentRows, int totalPages, int currentPage, int totalRecords) {
    }

    private record ServerConfig(String color, String colorDarker) {
    }

    private record UIComponents(String backToIndexButton, String searchModeratorSection) {
    }

    private record PunishmentRowData(String type, String playerUUID, String reason, String executorName, long time,
                                     long until, boolean isIpBan, int punishmentID, RemovedByData removedByData,
                                     String status) {
    }

    private record RemovedByData(String removedByName, Timestamp removedByDate) {
    }

    private record PunishmentStatus(String text, String badgeColorClass) {
    }
}