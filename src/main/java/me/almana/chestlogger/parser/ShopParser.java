package me.almana.chestlogger.parser;

import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.config.ModConfig;
import me.almana.chestlogger.data.ShopData;
import me.almana.chestlogger.service.GoogleSheetsService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopParser {

    private static final long BUY_ONLY_FLUSH_DELAY_MS = 200L;
    private static final int MAX_BUFFER_LINES = 20;
    private static final List<String> BUFFER = new ArrayList<>();
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\bfor\\s+([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENCHANT_PATTERN = Pattern.compile("^[A-Za-z ]+\\s+(X{0,3}(?:IX|IV|V?I{0,3}))$");
    private static final Pattern EFFECT_PATTERN = Pattern.compile("^\\(.+\\)$");
    private static boolean buffering = false;
    private static boolean seenItem = false;
    private static long flushAtMs = -1L;
    private static String shopLocation = "Unknown";

    private ShopParser() {
    }

    public static void handle(String message) {
        try {
            String line = message.trim();
            if (line.startsWith("Shop Information")) {
                flushIfComplete();
                BUFFER.clear();
                buffering = true;
                shopLocation = resolveShopLocation();
                flushAtMs = -1L;
                debug("Started shop buffer at {}", shopLocation);
            }

            if (!buffering) {
                return;
            }

            if (line.startsWith("Item:")) {
                seenItem = true;
            } else if (line.startsWith("Buy") || line.startsWith("Sell")) {
                seenItem = false;
            }

            if (!seenItem && !isPotentialShopLine(line)) {
                if (hasRequiredData(BUFFER)) {
                    flushIfComplete();
                }
                debug("Reset shop buffer on non-shop line: {}", line);
                reset();
                return;
            }

            BUFFER.add(line);
            if (line.startsWith("Sell")) {
                flushIfComplete();
                reset();
                return;
            }

            if (BUFFER.size() > MAX_BUFFER_LINES) {
                reset();
                return;
            }

            if (hasRequiredData(BUFFER)) {
                flushAtMs = System.currentTimeMillis() + BUY_ONLY_FLUSH_DELAY_MS;
                debug("Shop buffer ready. Scheduled flush in {}ms", BUY_ONLY_FLUSH_DELAY_MS);
            }
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Shop parser error", exception);
            reset();
        }
    }

    public static void tick() {
        try {
            if (!buffering || flushAtMs < 0L) {
                return;
            }

            if (System.currentTimeMillis() < flushAtMs) {
                return;
            }

            flushIfComplete();
            reset();
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Shop parser tick error", exception);
            reset();
        }
    }

    static ShopData parseLines(List<String> lines) {
        return parseLines(lines, "Unknown");
    }

    private static ShopData parseLines(List<String> lines, String location) {
        try {
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
            String extras = extractExtras(lines);
            if (!extras.isEmpty()) {
                item = item + " [" + extras + "]";
            }
            int buyPrice = buyLine == null ? 0 : parsePrice(buyLine);
            int sellPrice = sellLine == null ? 0 : parsePrice(sellLine);

            return new ShopData(owner, item, stock, buyPrice, sellPrice, location);
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Failed to parse shop block", exception);
            return null;
        }
    }

    private static boolean isPotentialShopLine(String line) {
        if (isShopInfoLine(line)) {
            return true;
        }

        return line.contains("Coins")
                || line.contains(":")
                || line.startsWith("-")
                || line.startsWith("*")
                || line.startsWith("Enchantment")
                || line.startsWith("Enchantments")
                || line.startsWith("Potion")
                || line.startsWith("Effects")
                || line.startsWith("Duration")
                || line.startsWith("Lore")
                || line.startsWith("(")
                || line.startsWith("[")
                || line.startsWith("Barrel")
                || ENCHANT_PATTERN.matcher(line).matches();
    }

    private static boolean isShopInfoLine(String line) {
        return line.startsWith("Shop Information")
                || line.startsWith("Owner:")
                || line.startsWith("Stock:")
                || line.startsWith("Item:")
                || line.startsWith("Buy")
                || line.startsWith("Sell");
    }

    private static boolean hasRequiredData(List<String> lines) {
        return findLine(lines, "Owner:") != null
                && findLine(lines, "Stock:") != null
                && findLine(lines, "Item:") != null
                && (findLine(lines, "Buy") != null || findLine(lines, "Sell") != null);
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

    private static String extractExtras(List<String> lines) {
        int itemIdx = -1;
        int buyIdx = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (itemIdx == -1 && l.startsWith("Item:")) {
                itemIdx = i;
            } else if (l.startsWith("Buy") || l.startsWith("Sell")) {
                buyIdx = i;
                break;
            }
        }
        if (itemIdx == -1) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = itemIdx + 1; i < buyIdx; i++) {
            String line = lines.get(i).trim();
            if (ENCHANT_PATTERN.matcher(line).matches()) {
                joiner.add(line);
            } else if (EFFECT_PATTERN.matcher(line).matches()) {
                joiner.add(line.substring(1, line.length() - 1));
            }
        }
        return joiner.toString();
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
        ShopData data = parseLines(BUFFER, shopLocation);
        if (data != null) {
            debug("Flushing shop owner={}, item={}, stock={}, buy={}, sell={}, location={}",
                    data.getOwner(),
                    data.getItem(),
                    data.getStock(),
                    data.getBuyPrice(),
                    data.getSellPrice(),
                    data.getShopLocation());
            GoogleSheetsService.logShop(data);
        } else {
            debug("Shop buffer could not be parsed: {}", BUFFER);
        }
    }

    private static String resolveShopLocation() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return "Unknown";
        }

        HitResult hitResult = client.crosshairTarget;
        if (!(hitResult instanceof BlockHitResult blockHitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return "Unknown";
        }

        BlockPos lookedAtPos = blockHitResult.getBlockPos();
        BlockPos containerPos = findContainerNear(client, lookedAtPos);
        return formatLocation(containerPos == null ? lookedAtPos : containerPos);
    }

    private static BlockPos findContainerNear(MinecraftClient client, BlockPos origin) {
        if (isContainer(client, origin)) {
            return origin;
        }

        for (Direction direction : Direction.values()) {
            BlockPos nearby = origin.offset(direction);
            if (isContainer(client, nearby)) {
                return nearby;
            }
        }
        return null;
    }

    private static boolean isContainer(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        Block block = state.getBlock();
        return block == Blocks.BARREL || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }

    private static String formatLocation(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void reset() {
        BUFFER.clear();
        buffering = false;
        seenItem = false;
        flushAtMs = -1L;
        shopLocation = "Unknown";
    }

    private static void debug(String message, Object... args) {
        if (ModConfig.get().isDebugLogging()) {
            ShopLoggerMod.LOGGER.info("[ChestLogger Debug] " + message, args);
        }
    }
}
