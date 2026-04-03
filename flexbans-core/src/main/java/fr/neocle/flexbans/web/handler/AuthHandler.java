package fr.neocle.flexbans.web.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.database.dashboard.RateLimiter;
import fr.neocle.flexbans.database.dashboard.SessionManager;
import fr.neocle.flexbans.database.dashboard.UserManager;
import fr.neocle.flexbans.logger.FlexLogger;
import fr.neocle.flexbans.util.permissions.FlexBansPermissionLookup;
import fr.neocle.flexbans.util.scheduler.TaskScheduler;
import fr.neocle.flexbans.util.player.UuidUsernameResolver;
import fr.neocle.flexbans.web.auth.TokenManager;
import fr.neocle.flexbans.web.response.ApiResponse;
import fr.neocle.flexbans.web.util.RequestValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthHandler {
    private final DatabaseUtils databaseUtils;
    private final UuidUsernameResolver resolver;
    private final RateLimiter rateLimiter;
    private final SessionManager sessionManager;

    private static final FlexLogger LOGGER = FlexLogger.get(AuthHandler.class);

    public AuthHandler(DatabaseUtils databaseUtils) {
        this.databaseUtils = databaseUtils;
        this.rateLimiter = databaseUtils.getRateLimiter();
        this.sessionManager = databaseUtils.getSessionManager();
        this.resolver = UuidUsernameResolver.get();

        startPeriodicCleanup();
    }

    private void startPeriodicCleanup() {
        TaskScheduler.get().runRepeating(
                () -> sessionManager.cleanupExpiredSessions()
                        .exceptionally(e -> {
                            LOGGER.error("Failed to cleanup expired sessions", e);
                            return 0;
                        }),
                3600000
        );
        TaskScheduler.get().runRepeating(
                () -> rateLimiter.cleanupOldEntries()
                        .exceptionally(e -> {
                            LOGGER.error("Failed to cleanup old rate limit entries", e);
                            return 0;
                        }),
                3600000
        );
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

    private String getClientIdentifier(HttpServletRequest req, String username) {
        String ip = req.getRemoteAddr();
        String userAgent = req.getHeader("User-Agent");
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

            return rateLimiter.isLockedOut(clientId, "login").thenCompose(lockedOut -> {
                if (lockedOut) {
                    return rateLimiter.getRemainingLockoutTime(clientId, "login").thenApply(remainingTime -> {
                        int minutes = (int) (remainingTime / 60000);

                        LOGGER.debug("Login attempt while rate limited {} from {}", username, req.getRemoteAddr());

                        return ApiResponse.tooManyRequests("Too many failed attempts. Try again in " + minutes + " minutes.");
                    });
                }

                UserManager userManager = databaseUtils.getUserManager();

                return userManager.isUserRegistered(username).thenCompose(isRegistered -> {
                    if (!isRegistered) {
                        rateLimiter.recordFailedAttempt(clientId, "login")
                                .exceptionally(e -> {
                                    LOGGER.error("Failed to record failed login attempt", e);
                                    return null;
                                });

                        LOGGER.debug("Login attempt for non-existent user {}", username);
                        return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid credentials"));
                    }

                    return userManager.validateCredentials(username, password).thenCompose(valid -> {
                        if (!valid) {
                            rateLimiter.recordFailedAttempt(clientId, "login")
                                    .exceptionally(e -> {
                                        LOGGER.error("Failed to record failed login attempt", e);
                                        return null;
                                    });

                            LOGGER.debug("Failed login attempt {} from {}", username, req.getRemoteAddr());
                            return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid credentials"));
                        }

                        rateLimiter.clearFailedAttempts(clientId, "login")
                                .exceptionally(e -> {
                                    LOGGER.error("Failed to clear failed attempts", e);
                                    return null;
                                });

                        return userManager.isUserVerified(username).thenCompose(isVerified ->
                                userManager.getUserInfo(username).thenCompose(userInfo -> {
                                    if (isVerified && userInfo != null) {
                                        return sessionManager.createSession(userInfo.id)
                                                .thenApply(sessionToken -> {
                                                    if (sessionToken != null) {
                                                        setSecureCookie(resp, "sessionToken", sessionToken, 86400, true);
                                                    }

                                                    String accessToken = TokenManager.get().createAccessToken(username);
                                                    String refreshToken = TokenManager.get().createRefreshToken(username);
                                                    setSecureCookie(resp, "accessToken", accessToken, 900, true);
                                                    setSecureCookie(resp, "refreshToken", refreshToken, 604800, true);

                                                    LOGGER.debug("Successful login {} from {}", username, req.getRemoteAddr());
                                                    return isVerified;
                                                });
                                    } else {
                                        String tempToken = TokenManager.get().createTempToken(username);
                                        setSecureCookie(resp, "tempToken", tempToken, 300, true);
                                        LOGGER.debug("2FA required for {}", username);
                                        return CompletableFuture.completedFuture(false);
                                    }
                                })
                        ).thenCompose(verified ->
                                getUserPermissions(username)
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
                        );
                    });
                });
            });
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Login error", e);
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    private static ApiResponse<JsonObject> internalError() {
        return ApiResponse.error(500, "Internal server error", null);
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
            UserManager userManager = databaseUtils.getUserManager();

            return rateLimiter.isLockedOut(clientId, "register").thenCompose(lockedOut -> {
                if (lockedOut) {
                    return CompletableFuture.completedFuture(
                            ApiResponse.tooManyRequests("Too many registration attempts. Try again later.")
                    );
                }

                return userManager.isUserRegistered(username).thenCompose(isRegistered -> {
                    if (isRegistered) {
                        rateLimiter.recordFailedAttempt(clientId, "register")
                                .exceptionally(e -> {
                                    LOGGER.error("Failed to record failed register attempt", e);
                                    return null;
                                });
                        return CompletableFuture.completedFuture(ApiResponse.conflict("User already exists"));
                    }

                    UUID uuid = resolver.usernameToUuid(username);
                    return userManager.registerUser(username, password, uuid).thenCompose(registered -> {
                        if (!registered) {
                            return CompletableFuture.completedFuture(ApiResponse.error(500, "Registration failed"));
                        }

                        rateLimiter.clearFailedAttempts(clientId, "register")
                                .exceptionally(e -> {
                                    LOGGER.error("Failed to clear register attempts", e);
                                    return null;
                                });

                        String tempToken = TokenManager.get().createTempToken(username);
                        setSecureCookie(resp, "tempToken", tempToken, 300, true);

                        LOGGER.debug("New user registered {}", username);

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
                                });
                    });
                });
            });
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Register error {}", e);
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public ApiResponse<?> handleRequest2FA(String token, HttpServletResponse resp) {
        try {
            if (!TokenManager.get().isValidToken(token, "temp") && !TokenManager.get().isValidToken(token, "access")) {
                return ApiResponse.unauthorized("Invalid or expired token");
            }

            String username = TokenManager.get().getUsernameFromToken(token);
            if (username == null) {
                return ApiResponse.unauthorized("Invalid or expired token");
            }

            boolean isAlreadyVerified = databaseUtils.getUserManager().isUserVerified(username).join();

            if (isAlreadyVerified) {
                JsonObject response = new JsonObject();
                response.addProperty("already_verified", true);
                response.addProperty("code", "");
                return ApiResponse.success(response);
            }

            String existingCode = databaseUtils.getUserManager().getVerificationCode(username).join();
            if (existingCode != null) {
                LOGGER.debug("Returning existing 2FA code for {}", username);
                JsonObject response = new JsonObject();
                response.addProperty("code", existingCode);
                response.addProperty("username", username);
                response.addProperty("already_verified", false);
                return ApiResponse.success(response, "Existing 2FA code returned");
            }

            if (!TokenManager.canAttemptVerification(username)) {
                LOGGER.debug("Too many 2FA attempts for {}", username);
                return ApiResponse.tooManyRequests("Too many verification attempts. Try again later.");
            }

            String code = generateSecure2FACode();
            databaseUtils.getUserManager().setVerificationCode(username, code).join();

            JsonObject response = new JsonObject();
            response.addProperty("code", code);
            response.addProperty("username", username);
            response.addProperty("already_verified", false);

            LOGGER.debug("2FA code generated for {}", username);
            return ApiResponse.success(response, "2FA code generated");
        } catch (Exception e) {
            LOGGER.error("2FA request error", e);
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public ApiResponse<?> handleCompleteVerification(String tempToken, HttpServletResponse resp) {
        try {
            if (!TokenManager.get().isValidToken(tempToken, "temp")) {
                return ApiResponse.unauthorized("Invalid or expired temp token");
            }

            String username = TokenManager.get().getUsernameFromToken(tempToken);
            if (username == null) {
                return ApiResponse.unauthorized("Invalid or expired temp token");
            }

            boolean isVerified = databaseUtils.getUserManager().isUserVerified(username).join();
            if (!isVerified) {
                return ApiResponse.unauthorized("User is not verified yet");
            }

            UserManager.UserInfo userInfo = databaseUtils.getUserManager().getUserInfo(username).join();
            if (userInfo != null) {
                String sessionToken = sessionManager.createSession(userInfo.id).join();
                if (sessionToken != null) {
                    setSecureCookie(resp, "sessionToken", sessionToken, 86400, true);
                }
            }

            String accessToken = TokenManager.get().createAccessToken(username);
            String refreshToken = TokenManager.get().createRefreshToken(username);
            setSecureCookie(resp, "accessToken", accessToken, 900, true);
            setSecureCookie(resp, "refreshToken", refreshToken, 604800, true);

            clearCookie(resp, "tempToken");

            LOGGER.debug("Verification complete and automatic login successful for {}", username);

            return ApiResponse.success(new JsonObject(), "Verification complete");

        } catch (Exception e) {
            LOGGER.error("Verification completion error", e);
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public CompletableFuture<ApiResponse<JsonObject>> handleGetMe(String token, HttpServletRequest req) {
        try {
            String type = TokenManager.get().getTokenType(token);
            if (!"access".equals(type) && !"temp".equals(type)) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            String username = TokenManager.get().getUsernameFromToken(token);
            if (username == null) {
                return CompletableFuture.completedFuture(ApiResponse.unauthorized("Invalid or expired token"));
            }

            UUID uuid = resolver.usernameToUuid(username);
            // uuid is only used to resolve permissions; getUserPermissions already does username->uuid internally,
            // but we'll keep this line if you use uuid elsewhere later.

            return databaseUtils.getUserManager().isUserVerified(username)
                    .thenCompose(isVerified ->
                            getUserPermissions(username).thenApply(permissions -> {
                                JsonObject response = new JsonObject();
                                response.addProperty("id", username);
                                response.addProperty("username", username);
                                response.addProperty("is_verified", isVerified);
                                response.add("permissions", permissions);

                                return ApiResponse.success(response);
                            })
                    )
                    .exceptionally(e -> {
                        LOGGER.error("Get me error", e);
                        return ApiResponse.error(500, "Internal server error");
                    });

        } catch (Exception e) {
            LOGGER.error("Get me error", e);
            return CompletableFuture.completedFuture(ApiResponse.error(500, "Internal server error"));
        }
    }

    public ApiResponse<?> handleLogout(String token, HttpServletRequest req, HttpServletResponse resp) {
        try {
            String username = null;

            if (token != null) {
                username = TokenManager.get().getUsernameFromToken(token);
                TokenManager.get().blacklistToken(token);
            }

            String sessionToken = getSessionTokenFromRequest(req);
            if (sessionToken != null) {
                sessionManager.deleteSession(sessionToken).join();
            }

            clearCookie(resp, "accessToken");
            clearCookie(resp, "refreshToken");
            clearCookie(resp, "tempToken");
            clearCookie(resp, "sessionToken");

            if (username != null) {
                //TokenManager.clearVerificationAttempts(username);
                LOGGER.debug("User logged out {} from {}", username, req.getRemoteAddr());
            }

            return ApiResponse.success(null, "Logged out successfully");
        } catch (Exception e) {
            LOGGER.error("Logout error", e);
            return ApiResponse.error(500, "Internal server error");
        }
    }

    public ApiResponse<?> handleRefresh(String refreshToken, HttpServletResponse resp) {
        try {
            if (!TokenManager.get().isValidToken(refreshToken, "refresh")) {
                return ApiResponse.unauthorized("Invalid or expired refresh token");
            }

            String username = TokenManager.get().getUsernameFromToken(refreshToken);
            if (username == null) {
                return ApiResponse.unauthorized("Invalid or expired refresh token");
            }

            TokenManager.get().blacklistToken(refreshToken);

            String newAccessToken = TokenManager.get().createAccessToken(username);
            String newRefreshToken = TokenManager.get().createRefreshToken(username);

            setSecureCookie(resp, "accessToken", newAccessToken, 900, true);
            setSecureCookie(resp, "refreshToken", newRefreshToken, 604800, true);

            JsonObject data = new JsonObject();
            data.addProperty("expiresIn", 900);
            return ApiResponse.success(data, "Token refreshed");
        } catch (Exception e) {
            LOGGER.error("Token refresh error", e);
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
        return password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    private String generateSecure2FACode() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    private boolean isValid2FACode(String code) {
        return code != null && code.matches("\\d{6}");
    }
}