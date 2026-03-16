package me.almana.chestlogger.service;

import com.google.gson.Gson;
import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.config.ModConfig;
import me.almana.chestlogger.data.PaymentData;
import me.almana.chestlogger.data.ShopData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

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

        String buySellType;
        if (data.getBuyPrice() > 0 && data.getSellPrice() > 0) {
            buySellType = "Both";
        } else if (data.getBuyPrice() > 0) {
            buySellType = "Buy";
        } else {
            buySellType = "Sell";
        }

        send(url, new ShopPayload(
                LocalDate.now().format(DATE_FORMATTER),
                resolveChestLocation(),
                resolvePlayerName(),
                data.getItem(),
                data.getStock(),
                buySellType,
                data.getOwner()
        ));
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

        long signedChange = "Withdraw".equalsIgnoreCase(data.getMovement())
                ? -Math.round(data.getAmount())
                : Math.round(data.getAmount());
        long treasury = Math.round(data.getNewBalance());

        send(url, new PaymentPayload(
                data.getDate().format(DATE_FORMATTER),
                treasury,
                signedChange,
                resolvePlayerName(),
                data.getMovement()
        ));
    }

    private static void send(String url, Object payload) {
        try {
            var wrapper = new java.util.HashMap<String, Object>();
            wrapper.put("apiKey", ModConfig.get().getApiKey());
            wrapper.put("data", payload);
            String json = GSON.toJson(wrapper);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            ShopLoggerMod.LOGGER.info("Sending payload to {}: {}", url, json);

            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        ShopLoggerMod.LOGGER.info("Response {}: {}", response.statusCode(), response.body());
                    })
                    .exceptionally(throwable -> {
                        ShopLoggerMod.LOGGER.warn("Failed to upload log payload", throwable);
                        return null;
                    });
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Failed to prepare log payload", exception);
        }
    }

    private static String resolvePlayerName() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getSession() != null) {
            return client.getSession().getUsername();
        }
        return "Unknown";
    }

    private static String resolveChestLocation() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            BlockPos pos = client.player.getBlockPos();
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        }
        return "Unknown";
    }

    // Sheet: Date | National treasure | Change amount | Player | Withdraw/Input
    private record PaymentPayload(
            String date,
            long treasury,
            long changeAmount,
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
