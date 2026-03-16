package me.almana.chestlogger;

import me.almana.chestlogger.chat.ChatListener;
import me.almana.chestlogger.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShopLoggerMod implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("chestlogger");

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ChatListener.register();
        LOGGER.info("ShopLogger initialized");
    }
}
