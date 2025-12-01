package fr.neocle.flexbans.handlers.security;

import fr.neocle.flexbans.configs.ConfigManager;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.utils.ResourceLoader;
import fr.neocle.flexbans.utils.security.CodeGenerator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class CodeVerificationHandler extends AbstractHandler {
    private final Logger logger;
    private final UserManager userManager;

    public CodeVerificationHandler(Logger logger, DatabaseUtils databaseUtils) {
        this.logger = logger;
        this.userManager = databaseUtils.getUserManager();
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
                if (userId != null) {
                    userManager.insertDiscordId(userId);
                    userManager.insertVerificationCodeFromDiscordId(userId, verificationCode);
                } else if (playerName != null) {
                    userManager.insertVerificationCodeFromPlayerName(playerName, verificationCode);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String serverIcon = ConfigManager.getString("server-display.icon");
            String serverFavicon = ConfigManager.getString("server-display.favicon");
            String serverLogo = ConfigManager.getString("server-display.logo");
            String serverColor = ConfigManager.getString("server-display.color");
            String serverColorDarker = ConfigManager.getString("server-display.darker-color");
            String serverName = ConfigManager.getString("server-display.name");

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

            String userId = (String) request.getSession().getAttribute("userId");
            String playerName = (String) request.getSession().getAttribute("playerName");
            String verificationCode = (String) request.getSession().getAttribute("verificationCode");
            boolean isVerified = false;

            if (verificationCode != null && userId != null) {
                isVerified = userManager.isUserVerified(userId);
                if (isVerified) {
                    request.getSession().setAttribute("verified", true);
                }
            } else if (verificationCode != null && playerName != null) {
                isVerified = userManager.isPlayerVerified(playerName);
                if (isVerified) {
                    request.getSession().setAttribute("verified", true);
                }
            }
            response.getWriter().write("{\"verified\": " + isVerified + "}");
        }
    }
}
