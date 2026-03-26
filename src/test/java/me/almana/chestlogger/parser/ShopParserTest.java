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

    @Test
    void parsesPotionShopWithExtraDetailLines() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: alchemist",
                "Stock: 128",
                "Item: [Potion of Fire Resistance]",
                "Effects: Fire Resistance",
                "Duration: 3:00",
                "Buy 1 for 120 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("alchemist", data.getOwner());
        assertEquals(128, data.getStock());
        assertEquals("Potion of Fire Resistance", data.getItem());
        assertEquals(120, data.getBuyPrice());
        assertEquals(0, data.getSellPrice());
    }

    @Test
    void parsesEnchantedItem() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: _Mutton",
                "Stock: 0",
                "Item: [Diamond Chestplate]",
                "Repair Cost: 1",
                "Protection Environmental IV",
                "Thorns II",
                "Durability III",
                "Buy 1 for 1625 Coins",
                "Sell 1 for 1500 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("_Mutton", data.getOwner());
        assertEquals(0, data.getStock());
        assertEquals("Diamond Chestplate [Protection Environmental IV, Thorns II, Durability III]", data.getItem());
        assertEquals(1625, data.getBuyPrice());
        assertEquals(1500, data.getSellPrice());
    }

    @Test
    void parsesPotionWithEffects() {
        List<String> lines = List.of(
                "Shop Information:",
                "Owner: _Mutton",
                "Stock: 41",
                "Item: [Dwarven Calling]",
                "Lore:",
                "Very Strong Alcoholic brew.",
                "(Strength II & Speed II ~3.30min)",
                "Barrel aged",
                "Buy 1 for 105 Coins",
                "Sell 1 for 100 Coins"
        );

        ShopData data = ShopParser.parseLines(lines);

        assertNotNull(data);
        assertEquals("_Mutton", data.getOwner());
        assertEquals(41, data.getStock());
        assertEquals("Dwarven Calling [Strength II & Speed II ~3.30min]", data.getItem());
        assertEquals(105, data.getBuyPrice());
        assertEquals(100, data.getSellPrice());
    }
}
