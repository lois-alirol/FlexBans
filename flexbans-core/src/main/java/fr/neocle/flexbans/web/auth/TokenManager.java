package fr.neocle.flexbans.web.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TokenManager {
    private static final Key SECRET_KEY = loadOrGenerateSecret();
    private static final long ACCESS_TOKEN_EXPIRY = 900000;
    private static final long REFRESH_TOKEN_EXPIRY = 604800000;
    private static final long TEMP_TOKEN_EXPIRY = 300000;
    private static final Map<String, TokenData> TOKEN_BLACKLIST = new ConcurrentHashMap<>();
    private static final Map<String, Integer> VERIFICATION_ATTEMPTS = new ConcurrentHashMap<>();
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final String SECRET_FILE = "plugins/FlexBans/jwt-secret.key";

    private static class TokenData {
        long expiryTime;
        String username;

        TokenData(long expiryTime, String username) {
            this.expiryTime = expiryTime;
            this.username = username;
        }
    }

    private static Key loadOrGenerateSecret() {
        try {
            File secretFile = new File(SECRET_FILE);
            if (secretFile.exists()) {
                String secret = Files.readString(secretFile.toPath()).trim();
                if (secret.length() >= 32) {
                    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                }
            }

            String newSecret = generateSecureSecret();
            secretFile.getParentFile().mkdirs();
            Files.writeString(secretFile.toPath(), newSecret);
            secretFile.setReadable(true, true);
            secretFile.setWritable(false, false);
            return Keys.hmacShaKeyFor(newSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Keys.hmacShaKeyFor(generateSecureSecret().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String generateSecureSecret() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    public static String createAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .claim("type", "access")
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String createRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String createTempToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TEMP_TOKEN_EXPIRY))
                .claim("type", "temp")
                .id(UUID.randomUUID().toString())
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                return null;
            }
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public static String getTokenType(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return (String) claims.get("type");
        } catch (JwtException e) {
            return null;
        }
    }

    public static String getTokenId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (JwtException e) {
            return null;
        }
    }

    public static boolean isValidToken(String token, String expectedType) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String username = getUsernameFromToken(token);
        if (username == null) {
            return false;
        }
        String tokenType = getTokenType(token);
        return expectedType.equals(tokenType);
    }

    public static void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long expiryTime = claims.getExpiration().getTime();
            TOKEN_BLACKLIST.put(token, new TokenData(expiryTime, claims.getSubject()));
        } catch (JwtException ignored) {}
    }

    public static boolean isTokenBlacklisted(String token) {
        return TOKEN_BLACKLIST.containsKey(token);
    }

    public static void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        TOKEN_BLACKLIST.entrySet().removeIf(entry -> entry.getValue().expiryTime < now);
        VERIFICATION_ATTEMPTS.clear();
    }

    public static boolean canAttemptVerification(String username) {
        int attempts = VERIFICATION_ATTEMPTS.getOrDefault(username, 0);
        if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
            return false;
        }
        VERIFICATION_ATTEMPTS.put(username, attempts + 1);
        return true;
    }

    public static void resetVerificationAttempts(String username) {
        VERIFICATION_ATTEMPTS.remove(username);
    }

    public static String getCurrentSecretHash() {
        try {
            return Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("SHA-256").digest(
                            SECRET_KEY.getEncoded()
                    )
            );
        } catch (java.security.NoSuchAlgorithmException e) {
            return "unknown";
        }
    }
}