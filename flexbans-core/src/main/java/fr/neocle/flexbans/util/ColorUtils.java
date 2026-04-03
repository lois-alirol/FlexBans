package fr.neocle.flexbans.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts MiniMessage-formatted strings (Adventure API) into
 * ANSI-escaped strings suitable for console output.
 *
 * Supported tags:
 *   Named colors  : <black>, <dark_blue>, <dark_green>, <dark_aqua>,
 *                   <dark_red>, <dark_purple>, <gold>, <gray>,
 *                   <dark_gray>, <blue>, <green>, <aqua>, <red>,
 *                   <light_purple>, <yellow>, <white>
 *   Hex colors    : <#RRGGBB>  (best-effort; mapped to nearest named color)
 *   Decorations   : <bold>, <italic>, <underlined>, <strikethrough>, <obfuscated>
 *   Reset         : <reset>
 *   Closing tags  : </tag>  (treated as reset for simplicity)
 *   Gradients     : <gradient:…> stripped (not representable in ANSI)
 *   Rainbow       : <rainbow> stripped
 *   Click / hover : <click:…>, <hover:…> stripped (not representable)
 */
public class MiniMessageToConsole {

    private static final String ANSI_RESET        = "\u001B[0m";
    private static final String ANSI_BOLD         = "\u001B[1m";
    private static final String ANSI_ITALIC       = "\u001B[3m";
    private static final String ANSI_UNDERLINE    = "\u001B[4m";
    private static final String ANSI_STRIKETHROUGH= "\u001B[9m";
    private static final String ANSI_OBFUSCATED   = "\u001B[5m";

    private static final String ANSI_BLACK        = "\u001B[30m";
    private static final String ANSI_DARK_BLUE    = "\u001B[34m";
    private static final String ANSI_DARK_GREEN   = "\u001B[32m";
    private static final String ANSI_DARK_AQUA    = "\u001B[36m";
    private static final String ANSI_DARK_RED     = "\u001B[31m";
    private static final String ANSI_DARK_PURPLE  = "\u001B[35m";
    private static final String ANSI_GOLD         = "\u001B[33m";
    private static final String ANSI_GRAY         = "\u001B[37m";
    private static final String ANSI_DARK_GRAY    = "\u001B[90m";
    private static final String ANSI_BLUE         = "\u001B[94m";
    private static final String ANSI_GREEN        = "\u001B[92m";
    private static final String ANSI_AQUA         = "\u001B[96m";
    private static final String ANSI_RED          = "\u001B[91m";
    private static final String ANSI_LIGHT_PURPLE = "\u001B[95m";
    private static final String ANSI_YELLOW       = "\u001B[93m";
    private static final String ANSI_WHITE        = "\u001B[97m";

    private static final Pattern TAG_PATTERN =
            Pattern.compile("</?([a-zA-Z0-9_#][^>]*)>", Pattern.CASE_INSENSITIVE);

    /**
     * Converts a MiniMessage string to an ANSI-colored console string.
     *
     * @param miniMessage the raw MiniMessage input
     * @return the string with ANSI escape codes, always ending with a reset
     */
    public static String toAnsi(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return "";

        StringBuilder result = new StringBuilder();
        Matcher matcher = TAG_PATTERN.matcher(miniMessage);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(miniMessage, lastEnd, matcher.start());
            lastEnd = matcher.end();

            String fullTag   = matcher.group(1).trim();
            boolean closing  = matcher.group(0).startsWith("</");

            String ansi = resolveTag(fullTag, closing);
            if (ansi != null) {
                result.append(ansi);
            }
        }

        result.append(miniMessage, lastEnd, miniMessage.length());

        result.append(ANSI_RESET);
        return result.toString();
    }

    /**
     * Strips all MiniMessage tags and returns plain text.
     *
     * @param miniMessage the raw MiniMessage input
     * @return plain text with no formatting
     */
    public static String stripFormatting(String miniMessage) {
        if (miniMessage == null) return "";
        return TAG_PATTERN.matcher(miniMessage).replaceAll("");
    }

    /**
     * Maps a tag name (without angle brackets) to an ANSI escape sequence.
     *
     * @param tag     the tag content, e.g. "bold", "red", "#FF5500", "click:…"
     * @param closing whether this is a closing tag (</…>)
     * @return the ANSI string, or null if the tag should be silently stripped
     */
    private static String resolveTag(String tag, boolean closing) {
        if (closing || tag.equalsIgnoreCase("reset")) {
            return ANSI_RESET;
        }

        String baseTag;
        String args;
        int colon = tag.indexOf(':');
        if (colon >= 0) {
            baseTag = tag.substring(0, colon).toLowerCase();
            args    = tag.substring(colon + 1);
        } else {
            baseTag = tag.toLowerCase();
            args    = "";
        }

        switch (baseTag) {
            case "bold":          return ANSI_BOLD;
            case "italic":        return ANSI_ITALIC;
            case "underlined":    return ANSI_UNDERLINE;
            case "strikethrough": return ANSI_STRIKETHROUGH;
            case "obfuscated":    return ANSI_OBFUSCATED;
        }

        switch (baseTag) {
            case "black":        return ANSI_BLACK;
            case "dark_blue":    return ANSI_DARK_BLUE;
            case "dark_green":   return ANSI_DARK_GREEN;
            case "dark_aqua":    return ANSI_DARK_AQUA;
            case "dark_red":     return ANSI_DARK_RED;
            case "dark_purple":  return ANSI_DARK_PURPLE;
            case "gold":         return ANSI_GOLD;
            case "gray":         return ANSI_GRAY;
            case "dark_gray":    return ANSI_DARK_GRAY;
            case "blue":         return ANSI_BLUE;
            case "green":        return ANSI_GREEN;
            case "aqua":         return ANSI_AQUA;
            case "red":          return ANSI_RED;
            case "light_purple": return ANSI_LIGHT_PURPLE;
            case "yellow":       return ANSI_YELLOW;
            case "white":        return ANSI_WHITE;
        }

        if (baseTag.startsWith("#") && baseTag.length() == 7) {
            return hexToAnsi(baseTag);
        }

        return null;
    }

    /**
     * Converts a hex color string (#RRGGBB) to the nearest ANSI named color.
     * Falls back to true-color ANSI (24-bit) if the terminal supports it;
     * otherwise maps to the closest Minecraft named color.
     *
     * This implementation uses true-color ANSI (supported by most modern
     * terminals: iTerm2, Windows Terminal, GNOME Terminal, …).
     *
     * @param hex a string like "#FF5500"
     * @return an ANSI escape sequence
     */
    private static String hexToAnsi(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
        } catch (NumberFormatException e) {
            return ANSI_RESET;
        }
    }
}
