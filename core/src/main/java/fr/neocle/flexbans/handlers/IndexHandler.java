package fr.neocle.flexbans.handlers;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.queries.DashboardQueries;
import fr.neocle.flexbans.exceptions.ConfigurationException;
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

    private static final int BANS_PAGE_SIZE = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.bans.max-per-page"));
    private static final int MUTES_PAGE_SIZE = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.mutes.max-per-page"));
    private static final int WARNINGS_PAGE_SIZE = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.warnings.max-per-page"));
    private static final int KICKS_PAGE_SIZE = Integer.parseInt((String) ConfigManager.getConfigValue("webserver.pages.punishments.kicks.max-per-page"));

    private final boolean usingFlexBans = HooksUtils.usingFlexBansSystem();
    private final boolean usingLiteBans = HooksUtils.usingLiteBansSystem();
    
    // Constants for repeated strings and patterns
    private static final String UUID_32_PATTERN = "^[0-9a-fA-F]{32}$";
    private static final String UUID_STANDARD_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";
    private static final String CONSOLE_IDENTIFIER = "[console]";
    private static final String CONSOLE_NAME = "Console";
    private static final String HTML_TEMPLATE_PATH = "web/index.html";
    private static final String REDIRECT_TO_INDEX = "/index";
    
    // Configuration holder inner class
    private static class ServerConfiguration {
        final String favicon;
        final String logo;
        final String color;
        final String colorDarker;
        final String name;
        final String description;
        
        ServerConfiguration() {
            this.favicon = (String) ConfigManager.getConfigValue("server-display.favicon");
            this.logo = (String) ConfigManager.getConfigValue("server-display.logo");
            this.color = (String) ConfigManager.getConfigValue("server-display.color");
            this.colorDarker = (String) ConfigManager.getConfigValue("server-display.darker-color");
            this.name = (String) ConfigManager.getConfigValue("server-display.name");
            this.description = (String) ConfigManager.getConfigValue("server-display.description");
        }
    }
    
    // Feature configuration holder inner class
    private static class FeatureConfiguration {
        final boolean bansEnabled;
        final boolean mutesEnabled;
        final boolean kicksEnabled;
        final boolean warningsEnabled;
        final boolean oauthEnabled;
        final boolean loginEnabled;
        final boolean searchPlayerEnabled;
        final boolean searchModeratorEnabled;
        final boolean searchPunishmentEnabled;
        final boolean punishmentExecutionEnabled;
        
        FeatureConfiguration() {
            this.bansEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.bans.enabled"));
            this.mutesEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.mutes.enabled"));
            this.kicksEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.kicks.enabled"));
            this.warningsEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.warnings.enabled"));
            this.oauthEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("discord-oauth.enabled"));
            this.loginEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("password-auth.enabled"));
            this.searchPlayerEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.player.enabled"));
            this.searchModeratorEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.moderator.enabled"));
            this.searchPunishmentEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.details.punishment.enabled"));
            this.punishmentExecutionEnabled = Boolean.parseBoolean((String) ConfigManager.getConfigValue("webserver.pages.punishments.punishment-execution-button"));
        }
    }

    public IndexHandler(UsernameUUIDConverters usernameUUIDConverters, PlayerHeadImage playerHeadImage, DatabaseUtils databaseUtils, Logger logger) {
        this.usernameUUIDConverters = usernameUUIDConverters;
        this.playerHeadImage = playerHeadImage;
        this.flexbansDatabase = databaseUtils;
        this.logger = logger;
    }
    
    /**
     * Validates and processes request parameters
     */
    private RequestParameters extractAndValidateParameters(HttpServletRequest request) {
        String type = request.getParameter("type");
        
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
        if (player != null && !player.matches(UUID_32_PATTERN) && !player.matches(UUID_STANDARD_PATTERN)) {
            player = usernameUUIDConverters.usernameToUUID(player);
        }
        
        String executor = request.getParameter("executor");
        if (executor != null && executor.equalsIgnoreCase(CONSOLE_IDENTIFIER)) {
            executor = CONSOLE_NAME;
        } else if (executor != null && (executor.matches(UUID_32_PATTERN) || executor.matches(UUID_STANDARD_PATTERN))) {
            executor = usernameUUIDConverters.UUIDtoUsername(executor);
        }
        
        String status = request.getParameter("status");
        String on = request.getParameter("on");
        String before = request.getParameter("before");
        String after = request.getParameter("after");
        
        return new RequestParameters(type, page, player, executor, status, on, before, after);
    }
    
    /**
     * Builds category navigation buttons HTML
     */
    private CategoryButtons buildCategoryButtons(FeatureConfiguration features) {
        String bansCategoryButton = "";
        String mutesCategoryButton = "";
        String kicksCategoryButton = "";
        String warningsCategoryButton = "";
        
        if (features.bansEnabled) {
            bansCategoryButton = """
                        <a href="?type=bans"
                           class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                           aria-label="View bans">
                            <i class="fas fa-ban"></i> <span class="ml-4 text-lg">Bans</span>
                            <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{bans_count}}</span>
                        </a>
                    """;
        }
        
        if (features.mutesEnabled) {
            mutesCategoryButton = """
                        <a href="?type=mutes"
                           class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                           aria-label="View mutes">
                            <i class="fas fa-microphone-slash"></i> <span class="ml-4 text-lg">Mutes</span>
                            <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{mutes_count}}</span>
                        </a>
                    """;
        }
        
        if (features.kicksEnabled) {
            kicksCategoryButton = """
                        <a href="?type=kicks"
                           class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                           aria-label="View kicks">
                            <i class="fas fa-user-times"></i> <span class="ml-4 text-lg">Kicks</span>
                            <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{kicks_count}}</span>
                        </a>
                    """;
        }
        
        if (features.warningsEnabled) {
            warningsCategoryButton = """
                        <a href="?type=warnings"
                           class="nav-item block py-2.5 px-4 mx-4 rounded-lg hover:bg-[#E6E6E6E6] dark:hover:bg-[#4B4B4BE6] relative"
                           aria-label="View warnings">
                            <i class="fas fa-exclamation-circle"></i> <span class="ml-4 text-lg">Warnings</span>
                            <span class="absolute right-3 top-3 inline-flex items-center justify-center w-10 h-6 bg-zinc-500 text-white text-x font-semibold rounded-full">{{warnings_count}}</span>
                        </a>
                    """;
        }
        
        return new CategoryButtons(bansCategoryButton, mutesCategoryButton, kicksCategoryButton, warningsCategoryButton);
    }
    
    /**
     * Builds search sections HTML
     */
    private SearchSections buildSearchSections(FeatureConfiguration features) {
        String usersDetailsSectionLabel = "";
        String searchPlayerSection = "";
        String searchModeratorSection = "";
        String punishmentDetailsSectionLabel = "";
        String searchPunishmentSection = "";
        
        if (features.searchPlayerEnabled || features.searchModeratorEnabled) {
            usersDetailsSectionLabel = """
                    <hr class="border-gray-600 my-4 w-3/4 mx-auto">
                    <h2 class="text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2">Users Details</h2>
                """;
        }
        
        if (features.searchPlayerEnabled) {
            searchPlayerSection = """
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
        
        if (features.searchModeratorEnabled) {
            String moderatorMarginClass = features.searchPlayerEnabled ? "mt-4" : "";
            searchModeratorSection = String.format("""
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
        
        if (features.searchPunishmentEnabled) {
            punishmentDetailsSectionLabel = """
                <hr class="border-gray-600 my-4 w-3/4 mx-auto">
                <h2 class="text-xl font-semibold text-[#333333] dark:text-[#e0e0e0] mb-2">Punishments Details</h2>
            """;
            
            searchPunishmentSection = """
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
        
        return new SearchSections(usersDetailsSectionLabel, searchPlayerSection, searchModeratorSection, 
                                 punishmentDetailsSectionLabel, searchPunishmentSection);
    }
    
    /**
     * Builds new punishment button HTML
     */
    private String buildNewPunishmentButton(FeatureConfiguration features, ServerConfiguration serverConfig) {
        if (features.punishmentExecutionEnabled && (features.oauthEnabled || features.loginEnabled)) {
            return "<hr class=\"border-gray-600 my-4 w-3/4 mx-auto\">" +
                   "<div class=\"flex w-full max-w-md space-x-2 justify-center\">" +
                   "<a href=\"/new-punishment\" class=\"nav-item block py-2.5 px-4 mx-4 rounded-lg text-white bg-[" + 
                   serverConfig.color + "] hover:bg-[" + serverConfig.colorDarker + "] relative\">" +
                   "<i class=\"fa-solid fa-gavel\"></i> <span class=\"ml-4 text-lg\">New Punishment</span>" +
                   "</a>" +
                   "</div>";
        }
        return "";
    }
    
    /**
     * Fetches all punishment counts
     */
    private PunishmentCounts fetchPunishmentCounts() throws SQLException {
        int bansCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "bans"));
        int mutesCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "mutes"));
        int kicksCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "kicks"));
        int warningsCount = getPunishmentCount(DashboardQueries.getTableName(usingFlexBans, usingLiteBans, "warnings"));
        
        return new PunishmentCounts(bansCount, mutesCount, kicksCount, warningsCount);
    }
    
    /**
     * Helper classes for data transfer
     */
    private static class RequestParameters {
        final String type;
        final int page;
        final String player;
        final String executor;
        final String status;
        final String on;
        final String before;
        final String after;
        
        RequestParameters(String type, int page, String player, String executor, String status, String on, String before, String after) {
            this.type = type;
            this.page = page;
            this.player = player;
            this.executor = executor;
            this.status = status;
            this.on = on;
            this.before = before;
            this.after = after;
        }
    }
    
    private static class CategoryButtons {
        final String bans;
        final String mutes;
        final String kicks;
        final String warnings;
        
        CategoryButtons(String bans, String mutes, String kicks, String warnings) {
            this.bans = bans;
            this.mutes = mutes;
            this.kicks = kicks;
            this.warnings = warnings;
        }
    }
    
    private static class SearchSections {
        final String usersDetailsSectionLabel;
        final String searchPlayerSection;
        final String searchModeratorSection;
        final String punishmentDetailsSectionLabel;
        final String searchPunishmentSection;
        
        SearchSections(String usersDetailsSectionLabel, String searchPlayerSection, String searchModeratorSection,
                      String punishmentDetailsSectionLabel, String searchPunishmentSection) {
            this.usersDetailsSectionLabel = usersDetailsSectionLabel;
            this.searchPlayerSection = searchPlayerSection;
            this.searchModeratorSection = searchModeratorSection;
            this.punishmentDetailsSectionLabel = punishmentDetailsSectionLabel;
            this.searchPunishmentSection = searchPunishmentSection;
        }
    }
    
    private static class PunishmentCounts {
        final int bans;
        final int mutes;
        final int kicks;
        final int warnings;
        
        PunishmentCounts(int bans, int mutes, int kicks, int warnings) {
            this.bans = bans;
            this.mutes = mutes;
            this.kicks = kicks;
            this.warnings = warnings;
        }
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (target.startsWith("/punishments") || target.startsWith("/index") || target.startsWith("/login")) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            if (!usingFlexBans && !usingLiteBans) {
                handleDatabaseError(request, response, "FlexBans is wrongly configured and is not using any punishments provider. " +
                        "If you are a server administrator, please check your config.");
                return;
            }

            FeatureConfiguration features = new FeatureConfiguration();
            Set<String> enabledTypes = getEnabledPunishmentTypes(features);
            
            if (enabledTypes.isEmpty()) {
                handleDatabaseError(request, response, "FlexBans is wrongly configured and all punishments are disabled in the webserver. " +
                        "If you are a server administrator, please check your config.");
                return;
            }

            RequestParameters params = extractAndValidateParameters(request);
            
            // Handle type parameter validation and redirect
            if (params.type == null || params.type.isEmpty()) {
                response.sendRedirect(request.getRequestURI() + "?type=" + enabledTypes.iterator().next());
                return;
            }
            
            if (!isValidPunishmentType(params.type, features)) {
                response.sendRedirect(REDIRECT_TO_INDEX);
                return;
            }

            String htmlTemplate = ResourceLoader.loadHtmlTemplate(HTML_TEMPLATE_PATH);
            if (htmlTemplate == null) {
                logger.warning("Unable to load HTML template for index page.");
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            try {
                ServerConfiguration serverConfig = new ServerConfiguration();
                PunishmentCounts counts = fetchPunishmentCounts();
                
                StringBuilder punishmentRows = new StringBuilder();
                int totalPages = processAndFetchPunishments(params, punishmentRows, request);
                
                String pageContent = buildFinalPageContent(htmlTemplate, features, serverConfig, 
                                                         params, counts, punishmentRows, totalPages);
                
                response.getWriter().write(pageContent);
                
            } catch (SQLException e) {
                e.printStackTrace();
                response.getWriter().write("Error: Unable to fetch punishment data.");
            }
        }
    }
    
    /**
     * Handles database configuration errors
     */
    private void handleDatabaseError(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        ConfigurationException exception = new ConfigurationException(message);
        request.setAttribute("javax.servlet.error.exception", exception);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Gets the set of enabled punishment types
     */
    private Set<String> getEnabledPunishmentTypes(FeatureConfiguration features) {
        Set<String> enabledTypes = new LinkedHashSet<>();
        if (features.bansEnabled) enabledTypes.add("bans");
        if (features.mutesEnabled) enabledTypes.add("mutes");
        if (features.kicksEnabled) enabledTypes.add("kicks");
        if (features.warningsEnabled) enabledTypes.add("warnings");
        return enabledTypes;
    }
    
    /**
     * Validates if punishment type is enabled
     */
    private boolean isValidPunishmentType(String type, FeatureConfiguration features) {
        return !((type.equalsIgnoreCase("bans") && !features.bansEnabled) ||
                (type.equalsIgnoreCase("mutes") && !features.mutesEnabled) ||
                (type.equalsIgnoreCase("kicks") && !features.kicksEnabled) ||
                (type.equalsIgnoreCase("warnings") && !features.warningsEnabled));
    }
    
    /**
     * Processes and fetches punishment data
     */
    private int processAndFetchPunishments(RequestParameters params, StringBuilder punishmentRows, 
                                         HttpServletRequest request) throws SQLException {
        String dbTable = DashboardQueries.getTableName(usingFlexBans, usingLiteBans, params.type);
        int totalPages = getTotalPages(dbTable, params.player, params.executor, params.status, 
                                     params.on, params.before, params.after, params.type);
        
        int adjustedPage = params.page;
        if (adjustedPage > totalPages && totalPages > 0) {
            adjustedPage = totalPages;
        }
        
        int pageSize = getPageSizeForType(params.type);
        int offset = (adjustedPage - 1) * pageSize;
        
        String query = DashboardQueries.buildPunishmentsQuery(
                usingFlexBans, dbTable, params.player, params.executor, params.status, 
                params.on, params.before, params.after, pageSize, offset
        );
        
        fetchAndAddPunishments(query, punishmentRows, params.type, false, params.player, 
                             params.executor, params.status, params.on, params.before, 
                             params.after, request);
        
        return totalPages;
    }
    
    /**
     * Builds the final page content with all replacements
     */
    private String buildFinalPageContent(String htmlTemplate, FeatureConfiguration features, 
                                       ServerConfiguration serverConfig, RequestParameters params, 
                                       PunishmentCounts counts, StringBuilder punishmentRows, int totalPages) {
        CategoryButtons categoryButtons = buildCategoryButtons(features);
        SearchSections searchSections = buildSearchSections(features);
        String newPunishmentButton = buildNewPunishmentButton(features, serverConfig);
        
        return htmlTemplate
                .replace("{{bans_category_button}}", categoryButtons.bans)
                .replace("{{mutes_category_button}}", categoryButtons.mutes)
                .replace("{{kicks_category_button}}", categoryButtons.kicks)
                .replace("{{warnings_category_button}}", categoryButtons.warnings)
                .replace("{{user_details_section_label}}", searchSections.usersDetailsSectionLabel)
                .replace("{{search_player_section}}", searchSections.searchPlayerSection)
                .replace("{{search_moderator_section}}", searchSections.searchModeratorSection)
                .replace("{{punishment_details_section_label}}", searchSections.punishmentDetailsSectionLabel)
                .replace("{{search_punishment_section}}", searchSections.searchPunishmentSection)
                .replace("{{new_punishment_button}}", newPunishmentButton)
                .replace("{{punishment_rows}}", punishmentRows.toString())
                .replace("{{punishment_type}}", params.type)
                .replace("{{punishment_type_capitalized}}", capitalize(params.type))
                .replace("{{server_name}}", serverConfig.name)
                .replace("{{server_description}}", serverConfig.description)
                .replace("{{server_favicon}}", serverConfig.favicon)
                .replace("{{server_color}}", serverConfig.color)
                .replace("{{server_color_hover}}", serverConfig.colorDarker)
                .replace("{{server_logo}}", serverConfig.logo)
                .replace("{{current_page}}", String.valueOf(params.page))
                .replace("{{total_pages}}", String.valueOf(totalPages))
                .replace("{{bans_count}}", String.valueOf(counts.bans))
                .replace("{{mutes_count}}", String.valueOf(counts.mutes))
                .replace("{{kicks_count}}", String.valueOf(counts.kicks))
                .replace("{{warnings_count}}", String.valueOf(counts.warnings));
    }

    private int getPageSizeForType(String type) {
        switch (type.toLowerCase()) {
            case "mutes":
                return MUTES_PAGE_SIZE;
            case "warnings":
                return WARNINGS_PAGE_SIZE;
            case "kicks":
                return KICKS_PAGE_SIZE;
            case "bans":
            default:
                return BANS_PAGE_SIZE;
        }
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
                                        String status, String on, String before, String after, HttpServletRequest request) throws SQLException {

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
                int offset = 0;
                String pageParam = request.getParameter("page");
                if (pageParam != null) {
                    try {
                        int page = Integer.parseInt(pageParam);
                        offset = (page - 1) * pageSize;
                    } catch (NumberFormatException e) {
                    }
                }
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