package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.queries.DashboardQueries;
import fr.neocle.flexbans.exceptions.ConfigurationException;
import fr.neocle.flexbans.handlers.cache.CountsCache;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class IndexHandler extends AbstractHandler {
    private final Logger logger;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils flexbansDatabase;

    private static final Map<String, Integer> PAGE_SIZES = Map.of(
            "bans", ConfigManager.getInt("webserver.pages.punishments.bans.max-per-page"),
            "mutes", ConfigManager.getInt("webserver.pages.punishments.mutes.max-per-page"),
            "warnings", ConfigManager.getInt("webserver.pages.punishments.warnings.max-per-page"),
            "kicks", ConfigManager.getInt("webserver.pages.punishments.kicks.max-per-page")
    );

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();

    public IndexHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage,
                        DatabaseUtils databaseUtils, Logger logger) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.flexbansDatabase = databaseUtils;
        this.logger = logger;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isValidTarget(target)) return;

        configureResponse(response, baseRequest);

        if (!isDatabaseConfigured()) {
            handleConfigurationError(request, response,
                    "FlexBans is wrongly configured and is not using any punishments provider.");
            return;
        }

        PunishmentConfig punishmentConfig = loadPunishmentConfig();
        if (punishmentConfig.enabledTypes.isEmpty()) {
            handleConfigurationError(request, response,
                    "FlexBans is wrongly configured and all punishments are disabled in the webserver.");
            return;
        }

        RequestParams params = extractRequestParams(request, punishmentConfig);
        if (params.redirectNeeded) {
            response.sendRedirect(params.redirectUrl);
            return;
        }

        if (!params.isValidType) {
            response.sendRedirect("/index");
            return;
        }

        String htmlTemplate = loadTemplate(response);
        if (htmlTemplate == null) return;

        PunishmentData data = fetchPunishmentData(params, request);
        if (data == null) {
            response.getWriter().write("Error: Unable to fetch punishment data.");
            return;
        }

        String pageContent = buildPageContent(htmlTemplate, params, data, punishmentConfig);
        response.getWriter().write(pageContent);
    }

    private boolean isValidTarget(String target) {
        return target.startsWith("/punishments") || target.startsWith("/index") || target.startsWith("/login");
    }

    private void configureResponse(HttpServletResponse response, Request baseRequest) {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }

    private boolean isDatabaseConfigured() {
        return usingFlexBans || usingLiteBans;
    }

    private void handleConfigurationError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        ConfigurationException exception = new ConfigurationException(message +
                " If you are a server administrator, please check your config.");
        request.setAttribute("javax.servlet.error.exception", exception);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private PunishmentConfig loadPunishmentConfig() {
        Map<String, Boolean> enabled = Map.of(
                "bans", ConfigManager.getBoolean("webserver.pages.punishments.bans.enabled"),
                "mutes", ConfigManager.getBoolean("webserver.pages.punishments.mutes.enabled"),
                "kicks", ConfigManager.getBoolean("webserver.pages.punishments.kicks.enabled"),
                "warnings", ConfigManager.getBoolean("webserver.pages.punishments.warnings.enabled")
        );

        Set<String> enabledTypes = new LinkedHashSet<>();
        enabled.entrySet().stream()
                .filter(Map.Entry::getValue)
                .forEach(entry -> enabledTypes.add(entry.getKey()));

        return new PunishmentConfig(enabled, enabledTypes);
    }

    private RequestParams extractRequestParams(HttpServletRequest request, PunishmentConfig config) {
        String type = request.getParameter("type");

        if (type == null || type.isEmpty()) {
            if (!config.enabledTypes.isEmpty()) {
                List<String> priority = List.of("bans", "mutes", "kicks", "warnings");
                String defaultType = priority.stream()
                        .filter(config.enabledTypes::contains)
                        .findFirst()
                        .orElse(config.enabledTypes.iterator().next());

                return new RequestParams(null, 0, true, "/index?type=" + defaultType, false, null, null, null, null, null, null);
            }
        }

        boolean isValidType = config.enabled.getOrDefault(type, false);
        if (!isValidType) {
            return new RequestParams(type, 0, false, null, false, null, null, null, null, null, null);
        }

        int page = parsePageParameter(request.getParameter("page"));
        String player = normalizePlayer(request.getParameter("player"));
        String executor = normalizeExecutor(request.getParameter("executor"));
        String status = request.getParameter("status");
        String on = request.getParameter("on");
        String before = request.getParameter("before");
        String after = request.getParameter("after");

        return new RequestParams(type, page, false, null, true, player, executor, status, on, before, after);
    }

    private int parsePageParameter(String pageParam) {
        if (pageParam == null) return 1;
        try {
            return Integer.parseInt(pageParam);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private String normalizePlayer(String player) {
        if (player == null) return null;
        if (!player.matches("^[0-9a-fA-F]{32}$") || !player.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")) {
            return usernameUUIDConverters.usernameToUUID(player);
        }
        return player;
    }

    private String normalizeExecutor(String executor) {
        if (executor == null) return null;
        if (executor.equalsIgnoreCase("[console]")) {
            return "Console";
        }
        if (executor.matches("^[0-9a-fA-F]{32}$") || executor.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")) {
            return usernameUUIDConverters.UUIDtoUsername(executor);
        }
        return executor;
    }

    private String loadTemplate(HttpServletResponse response) throws IOException {
        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/index.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for index page.");
            response.getWriter().write("Error: Unable to load HTML template.");
        }
        return htmlTemplate;
    }

    private PunishmentData fetchPunishmentData(RequestParams params, HttpServletRequest request) {
        try {
            String dbTable = DashboardQueries.getTableName(usingFlexBans, usingLiteBans, params.type);
            int totalPages = getTotalPages(dbTable, params.player, params.executor, params.status,
                    params.on, params.before, params.after, params.type);

            int adjustedPage = params.page;
            if (adjustedPage > totalPages && totalPages > 0) {
                adjustedPage = totalPages;
            }

            StringBuilder punishmentRows = new StringBuilder();

            if (totalPages == 0) {
                // For empty state, we'll return empty StringBuilder
                // The empty state will be handled in buildPageContent
            } else {
                int pageSize = getPageSizeForType(params.type);
                int offset = (adjustedPage - 1) * pageSize;

                String query = DashboardQueries.buildPunishmentsQuery(
                        usingFlexBans, dbTable, params.player, params.executor, params.status,
                        params.on, params.before, params.after, pageSize, offset
                );

                fetchAndAddPunishments(query, punishmentRows, params.type, false,
                        params.player, params.executor, params.status, params.on, params.before, params.after, request);
            }

            return new PunishmentData(punishmentRows, Math.max(totalPages, 1), adjustedPage,
                    CountsCache.bansCount, CountsCache.mutesCount, CountsCache.kicksCount, CountsCache.warningsCount);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildPageContent(String htmlTemplate, RequestParams params, PunishmentData data, PunishmentConfig config) {
        ServerConfig serverConfig = loadServerConfig();
        UIComponents components = buildUIComponents(config, serverConfig);

        boolean hasResults = data.punishmentRows.length() > 0 && !isEmptyStateMessage(data.punishmentRows.toString());
        String tableContent = hasResults ? buildTableHtml(data.punishmentRows.toString()) : "";
        String emptyStateContent = hasResults ? "" : buildEmptyStateSection(params.type);
        String paginationContent = hasResults ? buildPaginationHtml(data.currentPage, data.totalPages) : "";

        return htmlTemplate
                .replace("{{bans_category_button}}", components.bansCategoryButton)
                .replace("{{mutes_category_button}}", components.mutesCategoryButton)
                .replace("{{kicks_category_button}}", components.kicksCategoryButton)
                .replace("{{warnings_category_button}}", components.warningsCategoryButton)
                .replace("{{user_details_section_label}}", components.usersDetailsSectionLabel)
                .replace("{{search_player_section}}", components.searchPlayerSection)
                .replace("{{search_moderator_section}}", components.searchModeratorSection)
                .replace("{{punishment_details_section_label}}", components.punishmentDetailsSectionLabel)
                .replace("{{search_punishment_section}}", components.searchPunishmentSection)
                .replace("{{new_punishment_button}}", components.newPunishmentButton)
                .replace("{{table_content}}", tableContent)
                .replace("{{empty_state_content}}", emptyStateContent)
                .replace("{{pagination_content}}", paginationContent)
                .replace("{{punishment_type}}", params.type)
                .replace("{{punishment_type_capitalized}}", TextUtils.capitalize(params.type))
                .replace("{{server_name}}", serverConfig.name)
                .replace("{{server_description}}", serverConfig.description)
                .replace("{{server_favicon}}", serverConfig.favicon)
                .replace("{{server_color}}", serverConfig.color)
                .replace("{{server_color_hover}}", serverConfig.colorDarker)
                .replace("{{server_logo}}", serverConfig.logo)
                .replace("{{current_page}}", String.valueOf(data.currentPage))
                .replace("{{total_pages}}", String.valueOf(data.totalPages))
                .replace("{{bans_count}}", String.valueOf(data.bansCount))
                .replace("{{mutes_count}}", String.valueOf(data.mutesCount))
                .replace("{{kicks_count}}", String.valueOf(data.kicksCount))
                .replace("{{warnings_count}}", String.valueOf(data.warningsCount));
    }

    private String buildTableHtml(String punishmentRows) {
        return String.format("""
                <div class="punishments mx-4 overflow-x-auto">
                    <table id="punishmentTable"
                           class="table-auto w-full bg-[#ffffffe6] dark:bg-[#2c2c2ce6] text-[#333333] dark:text-[#e0e0e0] rounded-lg overflow-hidden shadow-custom">
                        <thead>
                        <tr class="bg-[#ccc] dark:bg-[#444] text-left text-slate-800 dark:text-slate-300">
                            <th class="p-4">Status</th>
                            <th class="p-4">Player</th>
                            <th class="p-4">Executor</th>
                            <th class="p-4">Reason</th>
                            <th class="p-4">Execution Date</th>
                            <th class="p-4">Expiration Time</th>
                            <th class="p-4">Duration</th>
                        </tr>
                        </thead>
                        <tbody>
                        %s
                        </tbody>
                    </table>
                </div>
                """, punishmentRows);
    }

    private String buildPaginationHtml(int currentPage, int totalPages) {
        if (totalPages <= 1) {
            return "";
        }

        return String.format("""
            <div class="pagination-controls flex justify-center my-4">
                <a id="prevPage" href="#"
                   class="pagination-btn prev-btn flex items-center justify-center bg-[#c1c1c1] dark:bg-[#444] text-white py-2 px-4 mx-2 rounded-lg">
                    <i class="fa-solid fa-angles-left"></i>
                </a>
            
                <input id="pageInput" type="number" min="1" max="%d" value="%d"
                       class="text-center bg-[#c1c1c1] dark:bg-[#444] text-black dark:text-white py-2 px-4 mx-2 rounded-lg w-20"
                       aria-label="Page Number">
            
                <a id="nextPage" href="#"
                   class="pagination-btn next-btn flex items-center justify-center bg-[#c1c1c1] dark:bg-[#444] text-white py-2 px-4 mx-2 rounded-lg">
                    <i class="fa-solid fa-angles-right"></i>
                </a>
            </div>
            """, totalPages, currentPage);
    }

    private ServerConfig loadServerConfig() {
        return new ServerConfig(
                ConfigManager.getString("server-display.favicon"),
                ConfigManager.getString("server-display.logo"),
                ConfigManager.getString("server-display.color"),
                ConfigManager.getString("server-display.darker-color"),
                ConfigManager.getString("server-display.name"),
                ConfigManager.getString("server-display.description")
        );
    }

    private String buildEmptyStateSection(String type) {
        return String.format("""
                <div class="flex flex-col items-center justify-center py-16 px-4">
                    <div class="empty-state-container text-center">
                        <i class="fas fa-search empty-state-icon text-8xl text-gray-400 dark:text-gray-600 mb-6"></i>
                        <h3 class="text-2xl font-bold text-gray-600 dark:text-gray-400 mb-3">No %s Found</h3>
                        <p class="text-lg text-gray-500 dark:text-gray-500 mb-4">There are currently no %s in the database.</p>
                        <div class="text-sm text-gray-400 dark:text-gray-600">
                            <i class="fas fa-info-circle mr-2"></i>
                            Try adjusting your search filters or check back later
                        </div>
                    </div>
                </div>
                """, TextUtils.capitalize(type), type);
    }

    private UIComponents buildUIComponents(PunishmentConfig config, ServerConfig serverConfig) {
        String bansCategoryButton = config.enabled.get("bans") ? createCategoryButton("bans", "fas fa-ban", "Bans", "{{bans_count}}") : "";
        String mutesCategoryButton = config.enabled.get("mutes") ? createCategoryButton("mutes", "fas fa-microphone-slash", "Mutes", "{{mutes_count}}") : "";
        String kicksCategoryButton = config.enabled.get("kicks") ? createCategoryButton("kicks", "fas fa-user-times", "Kicks", "{{kicks_count}}") : "";
        String warningsCategoryButton = config.enabled.get("warnings") ? createCategoryButton("warnings", "fas fa-exclamation-circle", "Warnings", "{{warnings_count}}") : "";

        boolean searchPlayerEnabled = ConfigManager.getBoolean("webserver.pages.details.player.enabled");
        boolean searchModeratorEnabled = ConfigManager.getBoolean("webserver.pages.details.moderator.enabled");
        boolean searchPunishmentEnabled = ConfigManager.getBoolean("webserver.pages.details.punishment.enabled");
        boolean punishmentExecutionEnabled = ConfigManager.getBoolean("webserver.pages.punishments.punishment-execution-button");
        boolean oauthEnabled = ConfigManager.getBoolean("discord-oauth.enabled");
        boolean loginEnabled = ConfigManager.getBoolean("password-auth.enabled");

        String usersDetailsSectionLabel = (searchPlayerEnabled || searchModeratorEnabled) ?
                "<hr class=\"border-gray-600 my-4 w-3/4 mx-auto\"><h2 class=\"text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2\">Users Details</h2>" : "";

        String searchPlayerSection = searchPlayerEnabled ? createSearchSection("playerInput", "Search Player...", "searchPlayerButton") : "";
        String searchModeratorSection = searchModeratorEnabled ? createSearchSection("moderatorInput", "Search Moderator...", "searchModeratorButton", searchPlayerEnabled ? "mt-4" : "") : "";

        String punishmentDetailsSectionLabel = searchPunishmentEnabled ?
                "<hr class=\"border-gray-600 my-4 w-3/4 mx-auto\"><h2 class=\"text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2\">Punishments Details</h2>" : "";

        String searchPunishmentSection = searchPunishmentEnabled ? createSearchSection("punishmentInput", "Search {{punishment_type_capitalized}} ID...", "searchPunishmentButton") : "";

        String newPunishmentButton = (punishmentExecutionEnabled && (oauthEnabled || loginEnabled)) ?
                createNewPunishmentButton(serverConfig.color, serverConfig.colorDarker) : "";

        return new UIComponents(bansCategoryButton, mutesCategoryButton, kicksCategoryButton, warningsCategoryButton,
                usersDetailsSectionLabel, searchPlayerSection, searchModeratorSection, punishmentDetailsSectionLabel,
                searchPunishmentSection, newPunishmentButton);
    }

    private String createCategoryButton(String type, String icon, String label, String count) {
        return String.format("""
                <a href="?type=%s"
                   class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                   aria-label="View %s">
                    <i class="%s"></i> <span class="ml-4 text-lg">%s</span>
                    <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">%s</span>
                </a>
                """, type, type, icon, label, count);
    }

    private String createSearchSection(String inputId, String placeholder, String buttonId) {
        return createSearchSection(inputId, placeholder, buttonId, "");
    }

    private String createSearchSection(String inputId, String placeholder, String buttonId, String extraClass) {
        return String.format("""
                <div class="flex w-full max-w-md space-x-2 %s">
                    <input type="text" id="%s" placeholder="%s"
                           class="w-4/5 p-2 bg-[#f0f0f0cc] dark:bg-[#3b3b3bcc] text-[#333333] dark:text-[#e0e0e0] rounded focus:outline-none focus:ring focus:ring-[{{server_color}}]">
                    <button id="%s"
                            class="w-1/5 flex items-center justify-center p-2 bg-[{{server_color}}] text-white rounded hover:bg-[{{server_color_hover}}]">
                        <i class="fa-solid fa-search"></i>
                    </button>
                </div>
                """, extraClass, inputId, placeholder, buttonId);
    }

    private String createNewPunishmentButton(String serverColor, String serverColorDarker) {
        return String.format("""
                <hr class="border-gray-600 my-4 w-3/4 mx-auto">
                <div class="flex w-full max-w-md space-x-2 justify-center">
                    <a href="/new-punishment" class="nav-item block py-2.5 px-4 mx-4 rounded-lg text-white bg-[%s] hover:bg-[%s] relative">
                        <i class="fa-solid fa-gavel"></i> <span class="ml-4 text-lg">New Punishment</span>
                    </a>
                </div>
                """, serverColor, serverColorDarker);
    }

    private boolean isEmptyStateMessage(String content) {
        return content.contains("No ") && content.contains(" Found");
    }

    private int getPageSizeForType(String type) {
        return PAGE_SIZES.getOrDefault(type.toLowerCase(), PAGE_SIZES.get("bans"));
    }

    private int getTotalPages(String dbTable, String player, String executor,
                              String status, String on, String before, String after, String type) throws SQLException {
        String query = DashboardQueries.buildCountFilteredPunishmentsQuery(
                usingFlexBans, dbTable, player, executor, status, on, before, after);

        try (PreparedStatement stmt = prepareStatement(query)) {
            setParameters(stmt, player, executor, status, on, before, after);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalRecords = rs.getInt(1);
                    int pageSize = getPageSizeForType(type);
                    return (int) Math.ceil((double) totalRecords / pageSize);
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

    private void setParameters(PreparedStatement stmt, String player, String executor, String status,
                               String on, String before, String after) throws SQLException {
        int paramIndex = 1;

        if (player != null && !player.isEmpty()) {
            stmt.setString(paramIndex++, player);
        }
        if (executor != null && !executor.isEmpty()) {
            stmt.setString(paramIndex++, executor);
        }

        if (status != null && !status.isEmpty()) {
            long now = System.currentTimeMillis();
            if ("active".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
                stmt.setLong(paramIndex++, now);
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
    }

    private void fetchAndAddPunishments(String query, StringBuilder punishmentRows, String type,
                                        boolean isExpired, String player, String executor,
                                        String status, String on, String before, String after,
                                        HttpServletRequest request) throws SQLException {

        try (PreparedStatement stmt = prepareStatement(query)) {
            setParameters(stmt, player, executor, status, on, before, after);

            int pageSize = getPageSizeForType(type);
            stmt.setInt(stmt.getParameterMetaData().getParameterCount() - 1, pageSize);

            if (stmt.getParameterMetaData().getParameterCount() > 1) {
                int offset = calculateOffset(request, pageSize);
                stmt.setInt(stmt.getParameterMetaData().getParameterCount(), offset);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows, isExpired, type);
                }
            }
        }
    }

    private int calculateOffset(HttpServletRequest request, int pageSize) {
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            try {
                int page = Integer.parseInt(pageParam);
                return (page - 1) * pageSize;
            } catch (NumberFormatException e) {
                // Fall through to return 0
            }
        }
        return 0;
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows, boolean isExpired, String type) throws SQLException {
        String uuid = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "targetUUID"));
        Map<String, String> usernameCache = new ConcurrentHashMap<>();
        String playerName = usernameCache.computeIfAbsent(uuid, usernameUUIDConverters::UUIDtoUsername);

        String executor = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "executor"));
        String reason = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "reason"));
        int punishmentID = rs.getInt(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "id"));
        long time = rs.getLong(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "time"));

        long until = calculateUntilTime(rs, time);
        PunishmentStatus punishmentStatus = determinePunishmentStatus(rs, until, type);

        String executionDate = formatDate(time);
        String expirationDate = formatExpirationDate(until, time);
        String duration = DurationCalculator.calculateDuration(time, until);

        String rowClass = isExpired
                ? "bg-light-expired-bans dark:bg-dark-expired-bans text-light-text dark:text-dark-text"
                : "bg-light-active-bans dark:bg-dark-active-bans text-light-text dark:text-dark-text";

        buildPunishmentRowHtml(punishmentRows, punishmentID, type, punishmentStatus, playerName,
                executor, reason, executionDate, expirationDate, duration, rowClass);
    }

    private long calculateUntilTime(ResultSet rs, long time) throws SQLException {
        if (usingLiteBans) {
            return rs.getLong("until");
        } else if (usingFlexBans) {
            long duration = rs.getLong("duration");
            return duration == -1 ? -1 : time + duration;
        }
        return 0;
    }

    private PunishmentStatus determinePunishmentStatus(ResultSet rs, long until, String type) throws SQLException {
        if (type.equals("kicks")) {
            return new PunishmentStatus("Active", "bg-green-500 text-white", false, false);
        }

        String removedByName = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "removed_by_name"));
        long removedTimeMillis = rs.getLong(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "removed_by_date"));

        boolean isExpiredByTime = (until != -1 && until != 0 && until < System.currentTimeMillis());
        boolean isManuallyRemoved = false;
        boolean isExplicitlyExpired = false;

        if (usingLiteBans) {
            if (removedByName != null) {
                if ("#expired".equals(removedByName)) {
                    isExplicitlyExpired = true;
                } else if (removedTimeMillis > 0 && removedTimeMillis < System.currentTimeMillis()) {
                    isManuallyRemoved = true;
                }
            }
        } else if (usingFlexBans) {
            String status = rs.getString("status");
            if ("expired".equalsIgnoreCase(status)) {
                isExplicitlyExpired = true;
                isExpiredByTime = true;
            } else if ("removed".equalsIgnoreCase(status)) {
                isManuallyRemoved = true;
            }
        }

        if (isManuallyRemoved) {
            return new PunishmentStatus("Removed", "bg-orange-500 text-white", true, false);
        } else if (isExplicitlyExpired || isExpiredByTime) {
            return new PunishmentStatus("Expired", "bg-red-500 text-white", false, true);
        } else {
            return new PunishmentStatus("Active", "bg-green-500 text-white", false, false);
        }
    }

    private String formatDate(long timestamp) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(timestamp));
    }

    private String formatExpirationDate(long until, long time) {
        return (until == -1 || until == 0 || until == time) ? "Never" : formatDate(until);
    }

    private void buildPunishmentRowHtml(StringBuilder punishmentRows, int punishmentID, String type,
                                        PunishmentStatus status, String playerName, String executor,
                                        String reason, String executionDate, String expirationDate,
                                        String duration, String rowClass) {
        punishmentRows.append("<tr class='cursor-pointer ").append(rowClass)
                .append("' onclick=\"window.location.href='/details/").append(TextUtils.unCapitalize(type))
                .append("/").append(punishmentID).append("';\">");

        punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                .append("<span class='px-2 inline-flex text-xs leading-5 font-semibold rounded-full ")
                .append(status.badgeColorClass).append("'>")
                .append(status.text).append("</span></td>");

        appendPlayerColumn(punishmentRows, playerName);
        appendPlayerColumn(punishmentRows, executor, "/moderator/");

        punishmentRows.append("<td class='px-6 py-4 max-w-xs overflow-hidden overflow-ellipsis'>")
                .append(reason).append("</td>");

        punishmentRows.append("<td class='px-6 py-4'>").append(executionDate).append("</td>");

        if (!type.equals("kicks")) {
            punishmentRows.append("<td class='px-6 py-4'>").append(expirationDate).append("</td>")
                    .append("<td class='px-6 py-4'>").append(duration).append("</td>");
        }

        punishmentRows.append("</tr>");
    }

    private void appendPlayerColumn(StringBuilder punishmentRows, String playerName) {
        appendPlayerColumn(punishmentRows, playerName, "/player/");
    }

    private void appendPlayerColumn(StringBuilder punishmentRows, String playerName, String linkPrefix) {
        try {
            punishmentRows.append("<td class='px-6 py-4 whitespace-nowrap'>")
                    .append("<img src='").append(playerHeadImage.getPlayerHeadUrl(playerName, "32"))
                    .append("' alt='Player Head' class='inline-block'> ")
                    .append("<a href='").append(linkPrefix).append(playerName)
                    .append("' onclick='event.stopPropagation();' class='hover:underline'>")
                    .append(playerName).append("</a></td>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateConfig(Map<String, Object> newConfig) {
        //TO DO
    }

    private record PunishmentConfig(Map<String, Boolean> enabled, Set<String> enabledTypes) {
    }

    private record RequestParams(String type, int page, boolean redirectNeeded, String redirectUrl, boolean isValidType,
                                 String player, String executor, String status, String on, String before,
                                 String after) {
    }

    private record PunishmentData(StringBuilder punishmentRows, int totalPages, int currentPage, int bansCount,
                                  int mutesCount, int kicksCount, int warningsCount) {
    }

    private record ServerConfig(String favicon, String logo, String color, String colorDarker, String name,
                                String description) {
    }

    private record UIComponents(String bansCategoryButton, String mutesCategoryButton, String kicksCategoryButton,
                                String warningsCategoryButton, String usersDetailsSectionLabel,
                                String searchPlayerSection, String searchModeratorSection,
                                String punishmentDetailsSectionLabel, String searchPunishmentSection,
                                String newPunishmentButton) {
    }

    private record PunishmentStatus(String text, String badgeColorClass, boolean isRemoved, boolean isExpired) {
    }
}