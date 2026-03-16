package me.almana.chestlogger.parser;

import me.almana.chestlogger.data.ShopData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShopParserTest {

    @Test
    void parsesShopInformationBlock() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: Nutton",
                "Stock: 134",
                "Item: [Spore Blossom]",
                "Buy 1 for 400 Coins",
                "Sell 1 for 395 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("Nutton", data.getOwner());
        assertEquals(134, data.getStock());
        assertEquals("Spore Blossom", data.getItem());
        assertEquals(400, data.getBuyPrice());
        assertEquals(395, data.getSellPrice());
    }

    @Test
    void parsesBuyOnlyShopInformationBlock() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: alsome_sauser",
                "Stock: 3456",
                "Item: [Bottle o' Enchanting]",
                "Buy 64 for 400 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("alsome_sauser", data.getOwner());
        assertEquals(3456, data.getStock());
        assertEquals("Bottle o' Enchanting", data.getItem());
        assertEquals(400, data.getBuyPrice());
        assertEquals(0, data.getSellPrice());
    }

    @Test
    void parsesSellOnlyShopInformationBlock() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: alsome_sauser",
                "Stock: 64",
                "Item: [Bottle o' Enchanting]",
                "Sell 64 for 330 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("alsome_sauser", data.getOwner());
        assertEquals(64, data.getStock());
        assertEquals("Bottle o' Enchanting", data.getItem());
        assertEquals(0, data.getBuyPrice());
        assertEquals(330, data.getSellPrice());
    }
}
