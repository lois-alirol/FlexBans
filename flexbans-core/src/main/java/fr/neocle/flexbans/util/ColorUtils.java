package fr.neocle.flexbans.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class ColorUtils {

    private ColorUtils() {}

    private static final String RESET         = "\u001B[0m";
    private static final String BOLD          = "\u001B[1m";
    private static final String ITALIC        = "\u001B[3m";
    private static final String UNDERLINE     = "\u001B[4m";
    private static final String STRIKETHROUGH = "\u001B[9m";
    private static final String OBFUSCATED    = "\u001B[5m";

    public static String toAnsi(Component component) {
        StringBuilder sb = new StringBuilder();
        renderComponent(component, sb);
        sb.append(RESET);
        return sb.toString();
    }

    private static void renderComponent(Component component, StringBuilder sb) {
        applyStyle(component.style(), sb);

        if (component instanceof TextComponent tc) {
            sb.append(tc.content());
        } else if (component instanceof TranslatableComponent tr) {
            sb.append(tr.key());
        }

        for (Component child : component.children()) {
            renderComponent(child, sb);
            sb.append(RESET);
            applyStyle(component.style(), sb);
        }

        sb.append(RESET);
    }

    private static void applyStyle(Style style, StringBuilder sb) {
        TextColor color = style.color();
        if (color != null) {
            sb.append(colorToAnsi(color));
        }

        if (style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE) {
            sb.append(BOLD);
        }
        if (style.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE) {
            sb.append(ITALIC);
        }
        if (style.decoration(TextDecoration.UNDERLINED) == TextDecoration.State.TRUE) {
            sb.append(UNDERLINE);
        }
        if (style.decoration(TextDecoration.STRIKETHROUGH) == TextDecoration.State.TRUE) {
            sb.append(STRIKETHROUGH);
        }
        if (style.decoration(TextDecoration.OBFUSCATED) == TextDecoration.State.TRUE) {
            sb.append(OBFUSCATED);
        }
    }

    private static String colorToAnsi(TextColor color) {
        return String.format("\u001B[38;2;%d;%d;%dm",
                color.red(), color.green(), color.blue());
    }
}