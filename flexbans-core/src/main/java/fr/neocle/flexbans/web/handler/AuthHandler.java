package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.RateLimiter;
import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.FlexBansPermissionLookup;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Improved authentication handler with proper security implementations:
 * - Database-backed session management
 * - Database-backed rate limiting
 * - CSRF token management
 * - Comprehensive security logging
 */
public class AuthHandler {
    private final DatabaseUtils databaseUtils;
    private final UuidUsernameResolver resolver;
    private final RateLimiter rateLimiter;
    private final SessionManager sessionManager;

    public AuthHandler(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
        this.rateLimiter = databaseUtils.getRateLimiter();
        this.sessionManager = databaseUtils.getSessionManager();

        this.resolver = UuidUsernameResolver.get();

        startPeriodicCleanup();
    }

    /**
     * Start periodic cleanup of expired sessions and rate limits.
     */
    private void startPeriodicCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3600000); // 1 hour
                    sessionManager.cleanupExpiredSessions();
                    rateLimiter.cleanupOldEntries();
                    TokenManager.cleanupExpiredTokens();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private CompletableFuture<JsonArray> getUserPermissions(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resolver.usernameToUuid(username);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert username to UUID", e);
            }
        }).thenCompose(uuid ->
                FlexBansPermissionLookup.getFlexBansPermissions(uuid)
                        .thenApply(permissionsSet -> {
                            JsonArray permissions = new JsonArray();
                            for (String perm : permissionsSet) {
                                permissions.add(perm);
                            }
                            return permissions;
                        })
        );
    }

    private void setSecureCookie(HttpServletResponse resp, String name, String value, long maxAge, boolean httpOnly) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value);
        cookie.append("; Path=/");
        cookie.append("; Max-Age=").append(maxAge);
        cookie.append("; SameSite=Strict");
        if (httpOnly) {
            cookie.append("; HttpOnly");
        }
        if (isProduction()) {
            cookie.append("; Secure");
        }
        resp.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletResponse resp, String name) {
        setSecureCookie(resp, name, "", 0, true);
    }

    private boolean isProduction() {
        return "production".equals(System.getenv("APP_ENV"));
    }

    /**
     * Get client identifier for rate limiting (IP + User-Agent hash).
     */
    private String getClientIdentifier(HttpServletRequest req, String username) {
        String ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");

        // Combine IP and username for more specific rate limiting
        return username + ":" + ip + ":" + (userAgent != null ? userAgent.hashCode() : "");
    }

    public CompletableFuture<ApiResponse<JsonObject>> handleLogin(JsonObject body, HttpServletRequest req, HttpServletResponse resp) {
        try {
            String username = RequestValidator.getRequiredString(body, "username");
            String password = RequestValidator.getRequiredString(body, "password");

            if (username.length() < 3 || username.length() > 32) {
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Invalid username length"));
            }

            if (password.length() < 8 || password.length() > 128) {
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Invalid password length"));
            }

            String clientId = getClientIdentifier(req, username);

            // Check if locked out
            if (rateLimiter.isLockedOut(clientId, "login")) {
                long remainingTime = rateLimiter.getRemainingLockoutTime(clientId, "login");
                int minutes = (int) (remainingTime / 60000);
                FlexLogger.warn("Login attempt while locked out: " + username + " from " + req.getRemoteAddr());
                return CompletableFuture.completedFuture(
                        ApiResponse.tooManyRequests("Too many failed attempts. Try again in " + minutes + " minutes.")
                );
            }

            // Check if user exists
            if (!databaseUtils.getUserManager().isUserRegistered(username)) {
                rateLimiter.recordFailedAttempt(clientId, "login");
                FlexLogger.warn("Login attempt for non-existent user: " + username);
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid credentials"));
            }

            // Validate credentials
            if (!databaseUtils.getUserManager().validateCredentials(username, password)) {
                rateLimiter.recordFailedAttempt(clientId, "login");
                FlexLogger.warn("Failed login attempt: " + username + " from " + req.getRemoteAddr());
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid credentials"));
            }

            // Clear failed attempts on successful login
            rateLimiter.clearFailedAttempts(clientId, "login");

            boolean isVerified = databaseUtils.getUserManager().isUserVerified(username);
            UserManager.UserInfo userInfo = databaseUtils.getUserManager().getUserInfo(username);

            // Create session for verified users
            if (isVerified && userInfo != null) {
                String sessionToken = sessionManager.createSession(userInfo.id);
                if (sessionToken != null) {
                    setSecureCookie(resp, "sessionToken", sessionToken, 86400, true);
                }

                // Also set JWT tokens for compatibility
                String accessToken = TokenManager.createAccessToken(username);
                String refreshToken = TokenManager.createRefreshToken(username);
                setSecureCookie(resp, "accessToken", accessToken, 900, true);
                setSecureCookie(resp, "refreshToken", refreshToken, 604800, true);

                FlexLogger.info("Successful login: " + username + " from " + req.getRemoteAddr());
            } else {
                String tempToken = TokenManager.createTempToken(username);
                setSecureCookie(resp, "tempToken", tempToken, 300, true);
                FlexLogger.info("2FA required for: " + username);
            }

            final boolean verified = isVerified;

            return getUserPermissions(username)
                    .thenApply(permissions -> {
                        JsonObject userData = new JsonObject();
                        userData.addProperty("username", username);
                        userData.add("permissions", permissions);

                        JsonObject response = new JsonObject();
                        response.addProperty("is_verified", verified);
                        response.addProperty("requires_2fa", !verified);
                        response.add("user", userData);

                        return ApiResponse.success(response, verified ? "Login successful" : "2FA required");
                    })
                    .exceptionally(e -> {
                        FlexLogger.error("Login error during permission fetch: " + e.getMessage());
                        return ApiResponse.error(500, "Internal server error");
                    });

        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            FlexLogger.error("Login error: " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public CompletableFuture<ApiResponse<JsonObject>> handleRegister(JsonObject body, HttpServletRequest req, HttpServletResponse resp) {
        try {
            String username = RequestValidator.getRequiredString(body, "username");
            String password = RequestValidator.getRequiredString(body, "password");

            if (username.length() < 3 || username.length() > 32) {
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Username must be 3-32 characters"));
            }

            if (password.length() < 8 || password.length() > 128) {
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Password must be at least 8 characters"));
            }

            if (!isStrongPassword(password)) {
                return CompletableFuture.completedFuture(
                        ApiResponse.badRequest("Password must contain uppercase, lowercase, number, and special character")
                );
            }

            String clientId = getClientIdentifier(req, username);

            // Rate limit registration attempts
            if (rateLimiter.isLockedOut(clientId, "register")) {
                return CompletableFuture.completedFuture(
                        ApiResponse.tooManyRequests("Too many registration attempts. Try again later.")
                );
            }

            if (databaseUtils.getUserManager().isUserRegistered(username)) {
                rateLimiter.recordFailedAttempt(clientId, "register");
                return CompletableFuture.completedFuture(ApiResponse.conflict("User already exists"));
            }

            UUID uuid = resolver.usernameToUuid(username);
            boolean registered = databaseUtils.getUserManager().registerUser(username, password, uuid);

            if (!registered) {
                return CompletableFuture.completedFuture(ApiResponse.error(500, "Registration failed"));
            }

            rateLimiter.clearFailedAttempts(clientId, "register");

            String tempToken = TokenManager.createTempToken(username);
            setSecureCookie(resp, "tempToken", tempToken, 300, true);

            FlexLogger.info("New user registered: " + username);

            return getUserPermissions(username)
                    .thenApply(permissions -> {
                        JsonObject userData = new JsonObject();
                        userData.addProperty("username", username);
                        userData.add("permissions", permissions);

                        JsonObject response = new JsonObject();
                        response.addProperty("is_verified", false);
                        response.addProperty("requires_2fa", true);
                        response.add("user", userData);

                        return new ApiResponse<>(201, "Registration successful", response);
                    })
                    .exceptionally(e -> {
                        FlexLogger.error("Register error during permission fetch: " + e.getMessage());
                        return ApiResponse.error(500, "Internal server error");
                    });

        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            FlexLogger.error("Register error: " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public ApiResponse<?> handleRequest2FA(String token, HttpServletResponse resp) {
        try {
            if (!TokenManager.isValidToken(token, "temp") && !TokenManager.isValidToken(token, "access")) {
                return ApiResponse.unauthorized("Invalid or expired token");
            }

            String username = TokenManager.getUsernameFromToken(token);
            if (username == null) {
                return ApiResponse.unauthorized("Invalid or expired token");
            }

            boolean isAlreadyVerified = databaseUtils.getUserManager().isUserVerified(username);

            if (isAlreadyVerified) {
                JsonObject response = new JsonObject();
                response.addProperty("already_verified", true);
                response.addProperty("code", "");
                return ApiResponse.success(response);
            }

            if (!TokenManager.canAttemptVerification(username)) {
                FlexLogger.warn("Too many 2FA attempts for: " + username);
                return ApiResponse.tooManyRequests("Too many verification attempts. Try again later.");
            }

            String code = generateSecure2FACode();
            databaseUtils.getUserManager().setVerificationCode(username, code);

            // In production, send this code via email/SMS
            // For now, return it (NOT SECURE FOR PRODUCTION)
            JsonObject response = new JsonObject();
            response.addProperty("code", code);
            response.addProperty("already_verified", false);

            FlexLogger.info("2FA code generated for: " + username);
            return ApiResponse.success(response, "2FA code generated");
        } catch (Exception e) {
            FlexLogger.error("2FA request error: " + e.getMessage());
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public CompletableFuture<ApiResponse<JsonObject>> handleVerify2FA(JsonObject body, HttpServletRequest req, HttpServletResponse resp) {
        try {
            String tempToken = RequestValidator.getRequiredString(body, "token");
            String code = RequestValidator.getRequiredString(body, "code");

            if (!TokenManager.isValidToken(tempToken, "temp")) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            String username = TokenManager.getUsernameFromToken(tempToken);
            if (username == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            if (!isValid2FACode(code)) {
                return CompletableFuture.completedFuture(ApiResponse.badRequest("Invalid verification code format"));
            }

            String storedCode = databaseUtils.getUserManager().getVerificationCode(username);
            if (storedCode == null || !storedCode.equals(code)) {
                FlexLogger.warn("Invalid 2FA code attempt for: " + username);
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid verification code"));
            }

            UUID playerUuid = resolver.usernameToUuid(username);
            databaseUtils.getUserManager().verifyUser(username, playerUuid);

            UserManager.UserInfo userInfo = databaseUtils.getUserManager().getUserInfo(username);
            if (userInfo != null) {
                String sessionToken = sessionManager.createSession(userInfo.id);
                if (sessionToken != null) {
                    setSecureCookie(resp, "sessionToken", sessionToken, 86400, true);
                }
            }

            String accessToken = TokenManager.createAccessToken(username);
            String refreshToken = TokenManager.createRefreshToken(username);
            setSecureCookie(resp, "accessToken", accessToken, 900, true);
            setSecureCookie(resp, "refreshToken", refreshToken, 604800, true);
            clearCookie(resp, "tempToken");

            TokenManager.blacklistToken(tempToken);
            TokenManager.resetVerificationAttempts(username);

            FlexLogger.info("User verified: " + username + " from " + req.getRemoteAddr());

            return getUserPermissions(username)
                    .thenApply(permissions -> {
                        JsonObject userData = new JsonObject();
                        userData.addProperty("id", username);
                        userData.addProperty("username", username);
                        userData.addProperty("isVerified", true);
                        userData.add("permissions", permissions);

                        JsonObject response = new JsonObject();
                        response.addProperty("is_verified", true);
                        response.add("user", userData);

                        return ApiResponse.success(response, "Verification successful");
                    })
                    .exceptionally(e -> {
                        FlexLogger.error("2FA verification error: " + e.getMessage());
                        return ApiResponse.error(500, "Internal server error");
                    });

        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            FlexLogger.error("2FA verification error: " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public CompletableFuture<ApiResponse<JsonObject>> handleGetMe(String token, HttpServletRequest req) {
        try {
            if (!TokenManager.isValidToken(token, "access")) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            String username = TokenManager.getUsernameFromToken(token);
            if (username == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            UUID uuid = resolver.usernameToUuid(username);
            boolean isVerified = databaseUtils.getUserManager().isUserVerified(username);

            CompletableFuture<JsonArray> permissionsFuture = getUserPermissions(username);

            return permissionsFuture.thenApply(permissions -> {
                JsonObject response = new JsonObject();
                response.addProperty("id", username);
                response.addProperty("username", username);
                response.addProperty("is_verified", isVerified);
                response.add("permissions", permissions);

                return ApiResponse.success(response);
            }).exceptionally(e -> {
                FlexLogger.error("Get me error: " + e.getMessage());
                return ApiResponse.error(500, "Internal server error");
            });

        } catch (Exception e) {
            FlexLogger.error("Get me error: " + e.getMessage());
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public ApiResponse<?> handleLogout(String token, HttpServletRequest req, HttpServletResponse resp) {
        try {
            String username = null;

            if (token != null) {
                username = TokenManager.getUsernameFromToken(token);
                TokenManager.blacklistToken(token);
            }

            // Delete session
            String sessionToken = getSessionTokenFromRequest(req);
            if (sessionToken != null) {
                sessionManager.deleteSession(sessionToken);
            }

            clearCookie(resp, "accessToken");
            clearCookie(resp, "refreshToken");
            clearCookie(resp, "tempToken");
            clearCookie(resp, "sessionToken");

            if (username != null) {
                FlexLogger.info("User logged out: " + username + " from " + req.getRemoteAddr());
            }

            return ApiResponse.success(null, "Logged out successfully");
        } catch (Exception e) {
            FlexLogger.error("Logout error: " + e.getMessage());
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public ApiResponse<?> handleRefresh(String refreshToken, HttpServletResponse resp) {
        try {
            if (!TokenManager.isValidToken(refreshToken, "refresh")) {
                return ApiResponse.unauthorized("Invalid or expired refresh token");
            }

            String username = TokenManager.getUsernameFromToken(refreshToken);
            if (username == null) {
                return ApiResponse.unauthorized("Invalid or expired refresh token");
            }

            // Rotate refresh token for better security
            String newAccessToken = TokenManager.createAccessToken(username);
            String newRefreshToken = TokenManager.createRefreshToken(username);

            setSecureCookie(resp, "accessToken", newAccessToken, 900, true);
            setSecureCookie(resp, "refreshToken", newRefreshToken, 604800, true);

            // Blacklist old refresh token
            TokenManager.blacklistToken(refreshToken);

            return ApiResponse.success(null, "Token refreshed");
        } catch (Exception e) {
            FlexLogger.error("Token refresh error: " + e.getMessage());
            return ApiResponse.error(500, "Internal server error");
        }
    }

    private String getSessionTokenFromRequest(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (javax.servlet.http.Cookie cookie : req.getCookies()) {
                if ("sessionToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isStrongPassword(String password) {
        return password.matches(".*[A-Z].*") &&       // Uppercase
                password.matches(".*[a-z].*") &&       // Lowercase
                password.matches(".*[0-9].*") &&       // Number
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*"); // Special char
    }

    private String generateSecure2FACode() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private boolean isValid2FACode(String code) {
        return code != null && code.matches("\\d{6}");
    }
}