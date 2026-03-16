package me.almana.chestlogger.data;

import java.time.LocalDate;

public final class PaymentData {

    private final LocalDate date;
    private final double amount;
    private final String movement;
    private final String landName;
    private final double newBalance;

    public PaymentData(LocalDate date, double amount, String movement, String landName, double newBalance) {
        this.date = date;
        this.amount = amount;
        this.movement = movement;
        this.landName = landName;
        this.newBalance = newBalance;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getMovement() {
        return movement;
    }

    public String getLandName() {
        return landName;
    }

    public double getNewBalance() {
        return newBalance;
    }
}
