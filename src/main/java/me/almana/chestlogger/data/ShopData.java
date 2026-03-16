package me.almana.chestlogger.data;

public final class ShopData {

    private final String owner;
    private final String item;
    private final int stock;
    private final int buyPrice;
    private final int sellPrice;

    public ShopData(String owner, String item, int stock, int buyPrice, int sellPrice) {
        this.owner = owner;
        this.item = item;
        this.stock = stock;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public String getOwner() {
        return owner;
    }

    public String getItem() {
        return item;
    }

    public int getStock() {
        return stock;
    }

    public int getBuyPrice() {
        return buyPrice;
    }

    public int getSellPrice() {
        return sellPrice;
    }
}
