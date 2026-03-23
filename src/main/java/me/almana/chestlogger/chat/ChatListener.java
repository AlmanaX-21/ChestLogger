package me.almana.chestlogger.chat;

import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.parser.PaymentParser;
import me.almana.chestlogger.parser.ShopParser;
import me.almana.chestlogger.util.ChatUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ChatListener {

    private static final long DUPLICATE_WINDOW_MS = 2_000L;
    private static final int MAX_RECENT_MESSAGES = 256;
    private static final Map<String, Long> RECENT_MESSAGES = new HashMap<>();

    private ChatListener() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> ShopParser.tick());

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            try {
                String raw = message.getString();
                String plain = ChatUtils.stripFormatting(raw);

                if (plain.isEmpty() || isDuplicate(plain)) {
                    return;
                }

                ShopParser.handle(plain);
                PaymentParser.handle(plain);
            } catch (Exception exception) {
                ShopLoggerMod.LOGGER.debug("Failed to process chat message", exception);
            }
        });
    }

    private static boolean isDuplicate(String message) {
        long now = System.currentTimeMillis();
        Long lastSeen = RECENT_MESSAGES.get(message);

        if (lastSeen != null && now - lastSeen <= DUPLICATE_WINDOW_MS) {
            return true;
        }

        RECENT_MESSAGES.put(message, now);
        if (RECENT_MESSAGES.size() > MAX_RECENT_MESSAGES) {
            cleanupOld(now);
        }
        return false;
    }

    private static void cleanupOld(long now) {
        Iterator<Map.Entry<String, Long>> iterator = RECENT_MESSAGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > DUPLICATE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }
}
