package fr.neocle.flexbans.util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generates unique 6-character hexadecimal identifiers from punishment type and numerical ID.
 * Format: [type_code(1)][hash(5)]
 * Example: "B2F4A8"
 */
public class PunishmentIdGenerator {

    private static final char[] TYPE_CODES = {'B', 'M', 'W', 'K'}; // BAN, MUTE, WARNING, KICK
    private static final String[] PUNISHMENT_TYPES = {"BAN", "MUTE", "WARNING", "KICK"};
    private static final int TOTAL_LENGTH = 6;

    /**
     * Generates a unique 6-character hexadecimal ID.
     *
     * @param punishmentType The type of punishment (BAN, MUTE, WARNING, KICK)
     * @param punishmentId   The numerical ID of the punishment
     * @return A unique 6-character hexadecimal string (e.g., "B2F4A8")
     */
    public static String generateId(String punishmentType, long punishmentId) {
        if (punishmentType == null || punishmentType.isEmpty()) {
            throw new IllegalArgumentException("Punishment type cannot be null or empty");
        }

        if (punishmentId < 0) {
            throw new IllegalArgumentException("Punishment ID must be non-negative");
        }

        String normalizedType = punishmentType. toUpperCase();

        // Get type code
        char typeCode = getTypeCode(normalizedType);

        try {
            // Combine type + ID for hashing
            String combined = normalizedType + punishmentId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());

            // Convert first 2. 5 bytes to 5 hex characters
            String hashHex = bytesToHex(hash).substring(0, 5).toUpperCase();

            return typeCode + hashHex;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validates a generated punishment ID format.
     *
     * @param punishmentId The hexadecimal ID to validate
     * @return true if the ID format is valid
     */
    public static boolean validateId(String punishmentId) {
        if (punishmentId == null || punishmentId.length() != TOTAL_LENGTH) {
            return false;
        }

        // Check type code
        char typeCode = punishmentId.charAt(0);
        boolean validType = typeCode == 'B' || typeCode == 'M' || typeCode == 'W' || typeCode == 'K';

        if (!validType) {
            return false;
        }

        // Check if rest are valid hex
        try {
            Long.parseLong(punishmentId.substring(1), 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Gets the punishment type from ID.
     *
     * @param punishmentId The hexadecimal ID
     * @return The punishment type (BAN, MUTE, WARNING, KICK)
     */
    public static String getTypeFromId(String punishmentId) {
        if (punishmentId == null || punishmentId.isEmpty()) {
            throw new IllegalArgumentException("Invalid punishment ID");
        }

        return switch (punishmentId.charAt(0)) {
            case 'B' -> "BAN";
            case 'M' -> "MUTE";
            case 'W' -> "WARNING";
            case 'K' -> "KICK";
            default -> throw new IllegalArgumentException("Unknown type code: " + punishmentId.charAt(0));
        };
    }

    /**
     * Gets the type code for a punishment type.
     */
    private static char getTypeCode(String punishmentType) {
        for (int i = 0; i < PUNISHMENT_TYPES.length; i++) {
            if (PUNISHMENT_TYPES[i].equals(punishmentType)) {
                return TYPE_CODES[i];
            }
        }
        throw new IllegalArgumentException("Invalid punishment type: " + punishmentType);
    }

    /**
     * Converts bytes to hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}