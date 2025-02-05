package fr.neocle.litebansweb.handlers.Security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import fr.neocle.litebansweb.utils.DatabaseUtils;
import fr.neocle.litebansweb.utils.ResourceLoader;
import fr.neocle.litebansweb.utils.Security.CodeGenerator;

public class CodeVerificationHandler extends AbstractHandler {
    private final Logger logger;
    private final Map<String, Object> config;
    private final DatabaseUtils databaseUtils;
    private final String userId;
    private final String playerName;

    public CodeVerificationHandler(Logger logger, Map<String, Object> config, CodeGenerator codeGenerator, DatabaseUtils databaseUtils) {
        this.logger = logger;
        this.config = config;
        this.databaseUtils = databaseUtils;

        this.userId = "";
        this.playerName = "";
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("/code-verification".equalsIgnoreCase(target)) {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);

            String htmlTemplate = ResourceLoader.loadHtmlTemplate("web/security/verify.html");
            if (htmlTemplate == null) {
                logger.warning("Unable to load HTML template for code verification.");
                response.getWriter().write("Error: Unable to load HTML template.");
                return;
            }

            String verificationCode = (String) request.getSession().getAttribute("verificationCode");

            if (verificationCode == null) {
                verificationCode = CodeGenerator.generateCode();
                request.getSession().setAttribute("verificationCode", verificationCode);
            } 

            String userId = (String) request.getSession().getAttribute("userId");
            String playerName = (String) request.getSession().getAttribute("playerName");
            try {
                if (userId != null && playerName == null) {
                    databaseUtils.insertDiscordId(userId);
                    databaseUtils.insertVerificationCodeFromDiscordId(userId, verificationCode);
                } else if (userId == null && playerName != null) {
                    databaseUtils.insertVerificationCodeFromPlayerName(playerName, verificationCode);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> serverDisplaySettings = (Map<String, Object>) config.get("server_display_settings");
            String serverName = String.valueOf(serverDisplaySettings.getOrDefault("name", "Example"));
            String serverIcon = String.valueOf(serverDisplaySettings.getOrDefault("icon", "https://i.imgur.com/iweixVA.png"));
            String serverFavicon = String.valueOf(serverDisplaySettings.getOrDefault("favicon", "https://i.imgur.com/iweixVA.png"));
            String serverLogo = String.valueOf(serverDisplaySettings.getOrDefault("logo", "https://i.imgur.com/iweixVA.png"));
            String serverColor = String.valueOf(serverDisplaySettings.getOrDefault("color", "#4097e7"));
            String serverColorDarker = String.valueOf(serverDisplaySettings.getOrDefault("darker_color", "#207dd2"));

            String pageContent = htmlTemplate
                    .replace("{{code}}", verificationCode)
                    .replace("{{1}}", verificationCode.substring(0, 1))
                    .replace("{{2}}", verificationCode.substring(1, 2))
                    .replace("{{3}}", verificationCode.substring(2, 3))
                    .replace("{{4}}", verificationCode.substring(3, 4))
                    .replace("{{5}}", verificationCode.substring(4, 5))
                    .replace("{{6}}", verificationCode.substring(5, 6))
                    .replace("{{server_name}}", serverName)
                    .replace("{{server_icon}}", serverIcon)
                    .replace("{{server_favicon}}", serverFavicon)
                    .replace("{{server_color}}", serverColor)
                    .replace("{{server_color_hover}}", serverColorDarker)
                    .replace("{{server_logo}}", serverLogo);

            response.getWriter().write(pageContent);

        } else if ("/check-verification".equalsIgnoreCase(target)) {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
        
            String verificationCode = (String) request.getSession().getAttribute("verificationCode");
            boolean isVerified = false;
        
            if (verificationCode != null && userId != null) {
                isVerified = databaseUtils.isUserVerified(userId);
                if (isVerified) {
                    request.getSession().setAttribute("verified", true);
                }
            } else if (verificationCode != null && playerName != null) {
                isVerified = databaseUtils.isPlayerVerified(playerName);
                if (isVerified) {
                    request.getSession().setAttribute("verified", true);
                }
            }            
            response.getWriter().write("{\"verified\": " + isVerified + "}");
        }
    }
}
