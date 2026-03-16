package me.almana.chestlogger.parser;

import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.data.ShopData;
import me.almana.chestlogger.service.GoogleSheetsService;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopParser {

    private static final List<String> BUFFER = new ArrayList<>();
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\bfor\\s+([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static boolean buffering = false;

    private ShopParser() {
    }

    public static void handle(String message) {
        try {
            String line = message.trim();
            if (line.contains("Shop Information")) {
                flushIfComplete();
                BUFFER.clear();
                buffering = true;
            }

            if (!buffering) {
                return;
            }

            if (!isShopInfoLine(line)) {
                flushIfComplete();
                reset();
                return;
            }

            BUFFER.add(line);
            if (line.startsWith("Sell")) {
                flushIfComplete();
                reset();
            } else if (BUFFER.size() > 8) {
                reset();
            }
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Shop parser error", exception);
            reset();
        }
    }

    static ShopData parseLines(List<String> lines) {
        try {
            // Shop messages arrive in a fixed multi-line block
            String ownerLine = findLine(lines, "Owner:");
            String stockLine = findLine(lines, "Stock:");
            String itemLine = findLine(lines, "Item:");
            String buyLine = findLine(lines, "Buy");
            String sellLine = findLine(lines, "Sell");

            if (ownerLine == null || stockLine == null || itemLine == null || (buyLine == null && sellLine == null)) {
                return null;
            }

            String owner = valueAfterColon(ownerLine);
            int stock = parseInt(valueAfterColon(stockLine));
            String item = normalizeItem(valueAfterColon(itemLine));
            int buyPrice = buyLine == null ? 0 : parsePrice(buyLine);
            int sellPrice = sellLine == null ? 0 : parsePrice(sellLine);

            return new ShopData(owner, item, stock, buyPrice, sellPrice);
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Failed to parse shop block", exception);
            return null;
        }
    }

    private static boolean isShopInfoLine(String line) {
        return line.startsWith("Shop Information")
                || line.startsWith("Owner:")
                || line.startsWith("Stock:")
                || line.startsWith("Item:")
                || line.startsWith("Buy")
                || line.startsWith("Sell");
    }

    private static String findLine(List<String> lines, String prefix) {
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        return null;
    }

    private static String valueAfterColon(String line) {
        int index = line.indexOf(':');
        if (index == -1 || index + 1 >= line.length()) {
            return "";
        }
        return line.substring(index + 1).trim();
    }

    private static String normalizeItem(String rawItem) {
        String result = rawItem.trim();
        if (result.startsWith("[") && result.endsWith("]") && result.length() > 1) {
            return result.substring(1, result.length() - 1).trim();
        }
        return result;
    }

    private static int parsePrice(String line) {
        Matcher matcher = PRICE_PATTERN.matcher(line);
        if (matcher.find()) {
            return parseInt(matcher.group(1).replace(",", ""));
        }
        return parseInt(line.replaceAll("[^0-9]", ""));
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.trim().replace(",", ""));
    }

    private static void flushIfComplete() {
        ShopData data = parseLines(BUFFER);
        if (data != null) {
            GoogleSheetsService.logShop(data);
        }
    }

    private static void reset() {
        BUFFER.clear();
        buffering = false;
    }
}
