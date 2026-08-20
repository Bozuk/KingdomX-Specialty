package org.kingdoms.specialties.items;

import org.bukkit.ChatColor;
import org.bukkit.Color;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colour handling for the item names/lores of {@code specialties.yml}.
 * Supports the usual {@code &a} codes and, on 1.16+, {@code &#rrggbb} hex codes.
 */
public final class TextUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final boolean HEX_SUPPORTED = hexSupported();

    private TextUtil() {}

    private static boolean hexSupported() {
        try {
            net.md_5.bungee.api.ChatColor.class.getMethod("of", String.class);
            return true;
        } catch (NoSuchMethodException | NoClassDefFoundError ex) {
            return false;
        }
    }

    public static String colorize(String text) {
        if (text == null) return null;

        if (HEX_SUPPORTED) {
            Matcher matcher = HEX_PATTERN.matcher(text);
            StringBuffer builder = new StringBuffer(text.length());
            while (matcher.find()) {
                matcher.appendReplacement(builder,
                        Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of('#' + matcher.group(1)).toString()));
            }
            matcher.appendTail(builder);
            text = builder.toString();
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /** Parses {@code #rrggbb} or {@code r,g,b}. */
    public static Color parseColor(String raw) {
        if (raw == null) return null;
        raw = raw.trim();

        if (raw.startsWith("#")) {
            try {
                return Color.fromRGB(Integer.parseInt(raw.substring(1), 16));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        String[] parts = raw.split("\\s*,\\s*");
        if (parts.length != 3) return null;
        try {
            return Color.fromRGB(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
