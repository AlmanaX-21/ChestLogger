package me.almana.chestlogger.service;

import com.google.gson.Gson;
import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.config.ModConfig;
import me.almana.chestlogger.data.PaymentData;
import me.almana.chestlogger.data.ShopData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class GoogleSheetsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Gson GSON = new Gson();

    private GoogleSheetsService() {
    }

    public static void logShop(ShopData data) {
        ModConfig config = ModConfig.get();
        if (data == null || !config.isEnabled()) {
            return;
        }

        String url = config.getShopSheetUrl();
        if (url.isBlank()) {
            ShopLoggerMod.LOGGER.warn("Shop sheet URL not configured");
            return;
        }
        if (!isWebUrl(url)) {
            ShopLoggerMod.LOGGER.warn("Shop sheet URL is invalid. Expected Apps Script web app URL, got: {}", url);
            return;
        }

        String buySellType;
        if (data.getBuyPrice() > 0 && data.getSellPrice() > 0 && data.getSellPrice() != 1) {
            buySellType = "Both";
        } else if (data.getBuyPrice() > 0) {
            buySellType = "Buy";
        } else {
            buySellType = "Sell";
        }

        String displayName = resolveDisplayName();
        debug("Shop parsed owner={}, item={}, stock={}, buy={}, sell={}, location={}",
                data.getOwner(),
                data.getItem(),
                data.getStock(),
                data.getBuyPrice(),
                data.getSellPrice(),
                data.getShopLocation());
        send(url, new ShopPayload(
                LocalDate.now().format(DATE_FORMATTER),
                data.getShopLocation(),
                displayName,
                data.getItem(),
                data.getStock(),
                buySellType,
                data.getOwner()
        ), "Shop log sent as " + displayName);
    }

    public static void logPayment(PaymentData data) {
        ModConfig config = ModConfig.get();
        if (data == null || !config.isEnabled()) {
            return;
        }

        String url = config.getPaymentSheetUrl();
        if (url.isBlank()) {
            ShopLoggerMod.LOGGER.warn("Payment sheet URL not configured");
            return;
        }
        if (!isWebUrl(url)) {
            ShopLoggerMod.LOGGER.warn("Payment sheet URL is invalid. Expected Apps Script web app URL, got: {}", url);
            return;
        }

        long signedChange = "Withdraw".equalsIgnoreCase(data.getMovement())
                ? -Math.round(data.getAmount())
                : Math.round(data.getAmount());
        long treasury = Math.round(data.getNewBalance());
        String displayName = resolveDisplayName();
        debug("Payment parsed land={}, amount={}, movement={}, treasury={}",
                data.getLandName(),
                data.getAmount(),
                data.getMovement(),
                data.getNewBalance());

        send(url, new PaymentPayload(
                data.getDate().format(DATE_FORMATTER),
                treasury,
                signedChange,
                "",
                displayName,
                data.getMovement()
        ), "Payment log sent as " + displayName);
    }

    private static void send(String url, Object payload, String successMessage) {
        try {
            var wrapper = new java.util.HashMap<String, Object>();
            wrapper.put("apiKey", ModConfig.get().getApiKey());
            wrapper.put("data", payload);
            String json = GSON.toJson(wrapper);
            debug("Outgoing payload: {}", GSON.toJson(payload));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            ShopLoggerMod.LOGGER.info("Sending payload to {}", url);

            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        ShopLoggerMod.LOGGER.info("Response {}: {}", response.statusCode(), response.body());
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            notifySuccess(successMessage);
                        } else {
                            ShopLoggerMod.LOGGER.warn("Upload rejected by endpoint. Status={}, body={}",
                                    response.statusCode(),
                                    response.body());
                        }
                    })
                    .exceptionally(throwable -> {
                        ShopLoggerMod.LOGGER.warn("Failed to upload log payload", throwable);
                        return null;
                    });
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.warn("Failed to prepare log payload for URL {}", url, exception);
        }
    }

    private static String resolveDisplayName() {
        String alias = ModConfig.get().getPlayerAlias();
        if (alias != null && !alias.isBlank()) {
            return alias;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null) {
            return client.getSession().getUsername();
        }
        return "Unknown";
    }

    private static void notifySuccess(String message) {
        if (!ModConfig.get().isShowSuccessMessage()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[ChestLogger] " + message), false);
            }
        });
    }

    private static boolean isWebUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private static void debug(String message, Object... args) {
        if (ModConfig.get().isDebugLogging()) {
            ShopLoggerMod.LOGGER.info("[ChestLogger Debug] " + message, args);
        }
    }

    // Sheet: Date | National treasury | Change amount | Tally difference | Player | Movement
    private record PaymentPayload(
            String date,
            long treasury,
            long changeAmount,
            String tallyDifference,
            String player,
            String movement
    ) {
    }

    // Sheet: Date | chest location | Logger | item | stock | Buy/Sell/Both | shop-owner
    private record ShopPayload(
            String date,
            String chestLocation,
            String logger,
            String item,
            int stock,
            String buySellType,
            String shopOwner
    ) {
    }
}
