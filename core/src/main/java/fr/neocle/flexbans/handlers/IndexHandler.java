package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.queries.DashboardQueries;
import fr.neocle.flexbans.exceptions.ConfigurationException;
import fr.neocle.flexbans.handlers.helpers.IndexPageConfig;
import fr.neocle.flexbans.utils.DurationCalculator;
import fr.neocle.flexbans.utils.HooksUtils;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class IndexHandler extends AbstractHandler {
    private final Logger logger;
    private final UsernameUUIDConverters usernameUUIDConverters;
    private final PlayerHeadImage playerHeadImage;
    private final DatabaseUtils flexbansDatabase;
    private final IndexPageConfig config;

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();

    public IndexHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage, DatabaseUtils databaseUtils, Logger logger) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.flexbansDatabase = databaseUtils;
        this.logger = logger;
        this.config = new IndexPageConfig();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/punishments") || target.startsWith("/index") || target.startsWith("/login")) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            try {
                RequestParams requestParams = processRequest(request, response);
                if (requestParams == null) {
                    return; // Error handled in processRequest
                }
                
                PunishmentData punishmentData = loadPunishmentData(requestParams);
                String htmlContent = buildHtmlTemplate(requestParams, punishmentData);
                
                writeResponse(response, htmlContent);
                
            } catch (Exception e) {
                logger.severe("Error processing index request: " + e.getMessage());
                e.printStackTrace();
                response.getWriter().write("Error: Unable to process request.");
            }
        }
    }

    /**
     * Process and validate request parameters.
     */
    private RequestParams processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!usingFlexBans && !usingLiteBans) {
            ConfigurationException exception = new ConfigurationException(
                    "FlexBans is wrongly configured and is not using any punishments provider. " +
                            "If you are a server administrator, please check your config.");

            request.setAttribute("javax.servlet.error.exception", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }

        Set<String> enabledTypes = getEnabledPunishmentTypes();
        
        String type = request.getParameter("type");
        if (type == null || type.isEmpty()) {
            if (!enabledTypes.isEmpty()) {
                type = enabledTypes.iterator().next();
                response.sendRedirect(request.getRequestURI() + "?type=" + type);
                return null;
            } else {
                response.getWriter().write("No punishment types are enabled.");
                return null;
            }
        }

        if (!isPunishmentTypeEnabled(type)) {
            response.sendRedirect("/index");
            return null;
        }

        if (!hasAnyPunishmentTypesEnabled()) {
            ConfigurationException exception = new ConfigurationException(
                    "FlexBans is wrongly configured and all punishments are disabled in the webserver. " +
                            "If you are a server administrator, please check your config.");

            request.setAttribute("javax.servlet.error.exception", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }

        int page = parsePageParameter(request);
        String player = processPlayerParameter(request);
        String executor = processExecutorParameter(request);

        return new RequestParams(
                type, page, player, executor,
                request.getParameter("status"),
                request.getParameter("on"),
                request.getParameter("before"),
                request.getParameter("after")
        );
    }
    
    /**
     * Load punishment data from database.
     */
    private PunishmentData loadPunishmentData(RequestParams params) throws SQLException {
        String dbTable = DashboardQueries.getTableName(usingFlexBans, usingLiteBans, params.type);
        
        int bansCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "bans"));
        int mutesCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "mutes"));
        int kicksCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "kicks"));
        int warningsCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "warnings"));

        int totalPages = getTotalPages(dbTable, params.player, params.executor, params.status, 
                                     params.on, params.before, params.after, params.type);

        if (params.page > totalPages && totalPages > 0) {
            params = params.withPage(totalPages);
        }

        int pageSize = config.getPageSizeForType(params.type);
        int offset = (params.page - 1) * pageSize;

        String query = DashboardQueries.buildPunishmentsQuery(
                usingFlexBans, dbTable, params.player, params.executor, params.status, 
                params.on, params.before, params.after, pageSize, offset
        );

        StringBuilder punishmentRows = new StringBuilder();
        fetchAndAddPunishments(query, punishmentRows, params.type, false, params.player, 
                             params.executor, params.status, params.on, params.before, params.after, params.page);

        return new PunishmentData(punishmentRows, totalPages, bansCount, mutesCount, kicksCount, warningsCount);
    }
    
    /**
     * Build complete HTML template with all components.
     */
    private String buildHtmlTemplate(RequestParams params, PunishmentData data) throws IOException {
        String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/index.html");
        if (htmlTemplate == null) {
            logger.warning("Unable to load HTML template for index page.");
            throw new IOException("Unable to load HTML template.");
        }

        HtmlComponents components = buildHtmlComponents();
        
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
                .replace("{{punishment_rows}}", data.punishmentRows.toString())
                .replace("{{punishment_type}}", params.type)
                .replace("{{punishment_type_capitalized}}", capitalize(params.type))
                .replace("{{server_name}}", config.serverName)
                .replace("{{server_description}}", config.serverDescription)
                .replace("{{server_favicon}}", config.serverFavicon)
                .replace("{{server_color}}", config.serverColor)
                .replace("{{server_color_hover}}", config.serverColorDarker)
                .replace("{{server_logo}}", config.serverLogo)
                .replace("{{current_page}}", String.valueOf(params.page))
                .replace("{{total_pages}}", String.valueOf(data.totalPages))
                .replace("{{bans_count}}", String.valueOf(data.bansCount))
                .replace("{{mutes_count}}", String.valueOf(data.mutesCount))
                .replace("{{kicks_count}}", String.valueOf(data.kicksCount))
                .replace("{{warnings_count}}", String.valueOf(data.warningsCount));
    }

    /**
     * Write final response to client.
     */
    private void writeResponse(HttpServletResponse response, String content) throws IOException {
        response.getWriter().write(content);
    }

    // Helper classes for better organization
    
    /**
     * Holds request parameters in a structured way.
     */
    private static class RequestParams {
        final String type;
        final int page;
        final String player;
        final String executor;
        final String status;
        final String on;
        final String before;
        final String after;
        
        RequestParams(String type, int page, String player, String executor, 
                     String status, String on, String before, String after) {
            this.type = type;
            this.page = page;
            this.player = player;
            this.executor = executor;
            this.status = status;
            this.on = on;
            this.before = before;
            this.after = after;
        }
        
        RequestParams withPage(int newPage) {
            return new RequestParams(type, newPage, player, executor, status, on, before, after);
        }
    }
    
    /**
     * Holds punishment data loaded from database.
     */
    private static class PunishmentData {
        final StringBuilder punishmentRows;
        final int totalPages;
        final int bansCount;
        final int mutesCount;
        final int kicksCount;
        final int warningsCount;
        
        PunishmentData(StringBuilder punishmentRows, int totalPages, 
                      int bansCount, int mutesCount, int kicksCount, int warningsCount) {
            this.punishmentRows = punishmentRows;
            this.totalPages = totalPages;
            this.bansCount = bansCount;
            this.mutesCount = mutesCount;
            this.kicksCount = kicksCount;
            this.warningsCount = warningsCount;
        }
    }
    
    /**
     * Holds HTML components for template replacement.
     */
    private static class HtmlComponents {
        final String bansCategoryButton;
        final String mutesCategoryButton;
        final String kicksCategoryButton;
        final String warningsCategoryButton;
        final String usersDetailsSectionLabel;
        final String searchPlayerSection;
        final String searchModeratorSection;
        final String punishmentDetailsSectionLabel;
        final String searchPunishmentSection;
        final String newPunishmentButton;
        
        HtmlComponents(String bansCategoryButton, String mutesCategoryButton, String kicksCategoryButton,
                      String warningsCategoryButton, String usersDetailsSectionLabel, String searchPlayerSection,
                      String searchModeratorSection, String punishmentDetailsSectionLabel, 
                      String searchPunishmentSection, String newPunishmentButton) {
            this.bansCategoryButton = bansCategoryButton;
            this.mutesCategoryButton = mutesCategoryButton;
            this.kicksCategoryButton = kicksCategoryButton;
            this.warningsCategoryButton = warningsCategoryButton;
            this.usersDetailsSectionLabel = usersDetailsSectionLabel;
            this.searchPlayerSection = searchPlayerSection;
            this.searchModeratorSection = searchModeratorSection;
            this.punishmentDetailsSectionLabel = punishmentDetailsSectionLabel;
            this.searchPunishmentSection = searchPunishmentSection;
            this.newPunishmentButton = newPunishmentButton;
        }
    }

    // Helper methods for request processing
    
    private Set<String> getEnabledPunishmentTypes() {
        Set<String> enabledTypes = new LinkedHashSet<>();
        if (config.bansEnabled) enabledTypes.add("bans");
        if (config.mutesEnabled) enabledTypes.add("mutes");
        if (config.kicksEnabled) enabledTypes.add("kicks");
        if (config.warningsEnabled) enabledTypes.add("warnings");
        return enabledTypes;
    }
    
    private boolean isPunishmentTypeEnabled(String type) {
        return (type.equalsIgnoreCase("bans") && config.bansEnabled) ||
               (type.equalsIgnoreCase("mutes") && config.mutesEnabled) ||
               (type.equalsIgnoreCase("kicks") && config.kicksEnabled) ||
               (type.equalsIgnoreCase("warnings") && config.warningsEnabled);
    }
    
    private boolean hasAnyPunishmentTypesEnabled() {
        return config.bansEnabled || config.mutesEnabled || config.kicksEnabled || config.warningsEnabled;
    }
    
    private int parsePageParameter(HttpServletRequest request) {
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            try {
                return Integer.parseInt(pageParam);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
    
    private String processPlayerParameter(HttpServletRequest request) {
        String player = request.getParameter("player");
        if (player != null && (!player.matches("^[0-9a-fA-F]{32}$") || !player.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
            player = usernameUUIDConverters.usernameToUUID(player);
        }
        return player;
    }
    
    private String processExecutorParameter(HttpServletRequest request) {
        String executor = request.getParameter("executor");
        if (executor != null && executor.equalsIgnoreCase("[console]")) {
            executor = "Console";
        } else if (executor != null && (executor.matches("^[0-9a-fA-F]{32}$") || executor.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"))) {
            executor = usernameUUIDConverters.UUIDtoUsername(executor);
        }
        return executor;
    }

    // HTML Component building methods
    
    /**
     * Build all HTML components for template replacement.
     */
    private HtmlComponents buildHtmlComponents() {
        String bansCategoryButton = buildBansCategoryButton();
        String mutesCategoryButton = buildMutesCategoryButton();
        String kicksCategoryButton = buildKicksCategoryButton();
        String warningsCategoryButton = buildWarningsCategoryButton();
        
        String usersDetailsSectionLabel = buildUsersDetailsSectionLabel();
        String searchPlayerSection = buildSearchPlayerSection();
        String searchModeratorSection = buildSearchModeratorSection();
        
        String punishmentDetailsSectionLabel = buildPunishmentDetailsSectionLabel();
        String searchPunishmentSection = buildSearchPunishmentSection();
        String newPunishmentButton = buildNewPunishmentButton();
        
        return new HtmlComponents(bansCategoryButton, mutesCategoryButton, kicksCategoryButton,
                                warningsCategoryButton, usersDetailsSectionLabel, searchPlayerSection,
                                searchModeratorSection, punishmentDetailsSectionLabel, 
                                searchPunishmentSection, newPunishmentButton);
    }
    
    private String buildBansCategoryButton() {
        if (!config.bansEnabled) {
            return "";
        }
        return """
                    <a href="?type=bans"
                       class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                       aria-label="View bans">
                        <i class="fas fa-ban"></i> <span class="ml-4 text-lg">Bans</span>
                        <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{bans_count}}</span>
                    </a>
                """;
    }
    
    private String buildMutesCategoryButton() {
        if (!config.mutesEnabled) {
            return "";
        }
        return """
                    <a href="?type=mutes"
                       class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                       aria-label="View mutes">
                        <i class="fas fa-microphone-slash"></i> <span class="ml-4 text-lg">Mutes</span>
                        <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{mutes_count}}</span>
                    </a>
                """;
    }
    
    private String buildKicksCategoryButton() {
        if (!config.kicksEnabled) {
            return "";
        }
        return """
                    <a href="?type=kicks"
                       class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                       aria-label="View kicks">
                        <i class="fas fa-user-times"></i> <span class="ml-4 text-lg">Kicks</span>
                        <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{kicks_count}}</span>
                    </a>
                """;
    }
    
    private String buildWarningsCategoryButton() {
        if (!config.warningsEnabled) {
            return "";
        }
        return """
                    <a href="?type=warnings"
                       class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                       aria-label="View warnings">
                        <i class="fas fa-exclamation-circle"></i> <span class="ml-4 text-lg">Warnings</span>
                        <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{warnings_count}}</span>
                    </a>
                """;
    }
    
    private String buildUsersDetailsSectionLabel() {
        if (config.searchPlayerEnabled || config.searchModeratorEnabled) {
            return """
                    <hr class="border-gray-600 my-4 w-3/4 mx-auto">
                    <h2 class="text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2">Users Details</h2>
                """;
        }
        return "";
    }
    
    private String buildSearchPlayerSection() {
        if (!config.searchPlayerEnabled) {
            return "";
        }
        return """
                <div class="flex w-full max-w-md space-x-2">
                    <input type="text" id="playerInput" placeholder="Search Player..."
                           class="w-4/5 p-2 bg-[#f0f0f0cc] dark:bg-[#3b3b3bcc] text-[#333333] dark:text-[#e0e0e0] rounded focus:outline-none focus:ring focus:ring-[{{server_color}}]">
                    <button id="searchPlayerButton"
                            class="w-1/5 flex items-center justify-center p-2 bg-[{{server_color}}] text-white rounded hover:bg-[{{server_color_hover}}]">
                        <i class="fa-solid fa-search"></i>
                    </button>
                </div>
            """;
    }
    
    private String buildSearchModeratorSection() {
        if (!config.searchModeratorEnabled) {
            return "";
        }
        String moderatorMarginClass = config.searchPlayerEnabled ? "mt-4" : "";
        return String.format("""
                <div class="flex w-full max-w-md space-x-2 %s">
                    <input type="text" id="moderatorInput" placeholder="Search Moderator..."
                           class="w-4/5 p-2 bg-[#f0f0f0cc] dark:bg-[#3b3b3bcc] text-[#333333] dark:text-[#e0e0e0] rounded focus:outline-none focus:ring focus:ring-[{{server_color}}]">
                    <button id="searchModeratorButton"
                            class="w-1/5 flex items-center justify-center p-2 bg-[{{server_color}}] text-white rounded hover:bg-[{{server_color_hover}}]">
                        <i class="fa-solid fa-search"></i>
                    </button>
                </div>
            """, moderatorMarginClass);
    }
    
    private String buildPunishmentDetailsSectionLabel() {
        if (config.searchPunishmentEnabled) {
            return """
                <hr class="border-gray-600 my-4 w-3/4 mx-auto">
                <h2 class="text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2">Punishments Details</h2>
            """;
        }
        return "";
    }
    
    private String buildSearchPunishmentSection() {
        if (!config.searchPunishmentEnabled) {
            return "";
        }
        return """
                <div class="flex w-full max-w-md space-x-2">
                    <input type="text" id="punishmentInput" placeholder="Search {{punishment_type_capitalized}} ID..."
                           class="w-4/5 p-2 bg-[#f0f0f0cc] dark:bg-[#3b3b3bcc] text-[#333333] dark:text-[#e0e0e0] rounded focus:outline-none focus:ring focus:ring-[{{server_color}}]">
                    <button id="searchPunishmentButton"
                            class="w-1/5 flex items-center justify-center p-2 bg-[{{server_color}}] text-white rounded hover:bg-[{{server_color_hover}}]">
                        <i class="fa-solid fa-search"></i>
                    </button>
                </div>
            """;
    }
    
    private String buildNewPunishmentButton() {
        if (config.punishmentExecutionEnabled && (config.oauthEnabled || config.loginEnabled)) {
            return "<hr class=\"border-gray-600 my-4 w-3/4 mx-auto\">" +
                    "<div class=\"flex w-full max-w-md space-x-2 justify-center\">" +
                    "<a href=\"/new-punishment\" class=\"nav-item block py-2.5 px-4 mx-4 rounded-lg text-white bg-[" + config.serverColor + "] hover:bg-[" + config.serverColorDarker + "] relative\">" +
                    "<i class=\"fa-solid fa-gavel\"></i> <span class=\"ml-4 text-lg\">New Punishment</span>" +
                    "</a>" +
                    "</div>";
        }
        return "";
    }

    // Existing database methods with minor modifications

    private int getPageSizeForType(String type) {
        return config.getPageSizeForType(type);
    }

    private int getPunishmentCount(String tableName) throws SQLException {
        String query = DashboardQueries.getCountPunishmentsQuery(usingFlexBans, tableName);
        PreparedStatement stmt;

        if (usingFlexBans) {
            stmt = flexbansDatabase.prepareStatement(query);
        } else if (usingLiteBans) {
            stmt = Database.get().prepareStatement(query);
        } else {
            logger.severe("No database system is active.");
            return 0;
        }

        try (stmt; ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    private int getTotalPages(String dbTable, String player, String executor,
                              String status, String on, String before, String after, String type) throws SQLException {

        String query = DashboardQueries.buildCountFilteredPunishmentsQuery(
                usingFlexBans, dbTable, player, executor, status, on, before, after
        );

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

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
            }
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            if (status != null && !status.isEmpty()) {
                long now = System.currentTimeMillis();
                switch (status.toLowerCase()) {
                    case "active":
                    case "expired":
                        stmt.setLong(paramIndex++, now);
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
                    int pageSize = getPageSizeForType(type);
                    return (int) Math.ceil((double) totalRecords / pageSize);
                }
            }
        }

        return 0;
    }

    private void fetchAndAddPunishments(String query, StringBuilder punishmentRows, String type,
                                        boolean isExpired, String player, String executor,
                                        String status, String on, String before, String after, int page) throws SQLException {

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

            if (player != null && !player.isEmpty()) {
                stmt.setString(paramIndex++, player);
            }
            if (executor != null && !executor.isEmpty()) {
                stmt.setString(paramIndex++, executor);
            }

            if (status != null && !status.isEmpty()) {
                switch (status.toLowerCase()) {
                    case "active", "expired":
                        if (usingLiteBans) stmt.setLong(paramIndex++, System.currentTimeMillis());
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

            int pageSize = getPageSizeForType(type);
            stmt.setInt(paramIndex++, pageSize);

            if (paramIndex <= stmt.getParameterMetaData().getParameterCount()) {
                int offset = (page - 1) * pageSize;
                stmt.setInt(paramIndex++, offset);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    addPunishmentRow(rs, punishmentRows, isExpired, type);
                }
            }
        }
    }

    private void addPunishmentRow(ResultSet rs, StringBuilder punishmentRows, boolean isExpired, String type) throws SQLException {
        String uuid = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "targetUUID"));
        String playerName = usernameUUIDConverters.UUIDtoUsername(uuid);
        String executor = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "executor"));
        String reason = rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "reason"));

        int punishmentID = rs.getInt(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "id"));
        long time = rs.getLong(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "time"));

        long until = 0;
        if (usingLiteBans) {
            until = rs.getLong("until");
        } else if (usingFlexBans) {
            if (-1 == rs.getLong("duration")) {
                until = -1;
            } else {
                until = time + rs.getLong("duration");
            }
        }

        String removedByName = type.equals("kicks") ? "null" :
                rs.getString(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "removed_by_name"));

        Timestamp removedByDate = rs.getTimestamp(DashboardQueries.getColumnName(usingFlexBans, usingLiteBans, "removed_by_date"));

        boolean isExpiredByTime = false;
        boolean isManuallyRemoved = false;
        boolean isExplicitlyExpired = false;

        if (usingLiteBans) {
            if (until != -1 && until != 0 && until < System.currentTimeMillis()) {
                isExpiredByTime = true;
            }
        }

        if (usingLiteBans) {
            if (removedByName != null) {
                if ("#expired".equals(removedByName)) {
                    isExplicitlyExpired = true;
                } else if (removedByDate != null && removedByDate.before(new java.util.Date())) {
                    isManuallyRemoved = true;
                }
            }
        } else if (usingFlexBans) {
            if ("expired".equalsIgnoreCase(rs.getString("status"))) {
                isExplicitlyExpired = true;
                isExpiredByTime = true;
            } else if ("removed".equalsIgnoreCase(rs.getString("status"))) {
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
        String expirationDate = (until == -1 || until == 0 || until == time) ? "Never" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(until));

        String duration = DurationCalculator.calculateDuration(time, until);

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
        //TO DO
    }
}