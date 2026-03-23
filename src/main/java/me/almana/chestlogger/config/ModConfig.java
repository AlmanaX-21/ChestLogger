package me.almana.chestlogger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.almana.chestlogger.ShopLoggerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "chestlogger.json";
    private static ModConfig instance;

    private String shopSheetUrl = "";
    private String paymentSheetUrl = "";
    private String apiKey = "";
    private String playerAlias = "";
    private boolean showSuccessMessage = true;
    private boolean enabled = true;

    public static ModConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                instance = GSON.fromJson(reader, ModConfig.class);
                if (instance == null) {
                    instance = new ModConfig();
                }
                ShopLoggerMod.LOGGER.info("Loaded config from {}", path);
            } catch (Exception e) {
                ShopLoggerMod.LOGGER.warn("Failed to read config, using defaults", e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            save();
            ShopLoggerMod.LOGGER.info("Created default config at {}", path);
        }
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance != null ? instance : new ModConfig(), writer);
            }
        } catch (IOException e) {
            ShopLoggerMod.LOGGER.warn("Failed to save config", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public String getShopSheetUrl() {
        return shopSheetUrl;
    }

    public String getPaymentSheetUrl() {
        return paymentSheetUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getPlayerAlias() {
        return playerAlias;
    }

    public boolean isShowSuccessMessage() {
        return showSuccessMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
