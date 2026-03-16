package me.almana.chestlogger.util;

import java.util.regex.Pattern;

public final class ChatUtils {

    private static final Pattern FORMATTING_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");

    private ChatUtils() {
    }

    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = FORMATTING_PATTERN.matcher(text).replaceAll("");
        return cleaned.replace("Â§", "").trim();
    }
}
