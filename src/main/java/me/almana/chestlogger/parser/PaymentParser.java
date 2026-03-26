package me.almana.chestlogger.parser;

import me.almana.chestlogger.ShopLoggerMod;
import me.almana.chestlogger.config.ModConfig;
import me.almana.chestlogger.data.PaymentData;
import me.almana.chestlogger.service.GoogleSheetsService;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentParser {

    private static final long PENDING_TIMEOUT_MS = 5_000L;
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "^\\[Lands\\]\\s+You successfully\\s+(put|took|withdrew|withdraw)\\s+\\$([0-9,]+(?:\\.[0-9]+)?)\\s+in the bank of land\\s+([^.]+)\\.(?:\\s+New balance of the bank:\\s*\\$([0-9,]+(?:\\.[0-9]+)?))?\\s*$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BALANCE_PATTERN = Pattern.compile(
            "^New balance of the bank:\\s*\\$([0-9,]+(?:\\.[0-9]+)?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static PendingPayment pendingPayment;

    private PaymentParser() {
    }

    public static void handle(String message) {
        try {
            clearExpiredPending();

            String line = message.trim();
            ParsedTransaction transaction = parseTransactionLine(line);
            if (transaction != null) {
                if (transaction.newBalance() != null) {
                    debug("Parsed single-line payment land={}, amount={}, movement={}, balance={}",
                            transaction.landName(),
                            transaction.amount(),
                            transaction.movement(),
                            transaction.newBalance());
                    PaymentData payment = new PaymentData(
                            LocalDate.now(),
                            transaction.amount(),
                            transaction.movement(),
                            transaction.landName(),
                            transaction.newBalance()
                    );
                    GoogleSheetsService.logPayment(payment);
                    pendingPayment = null;
                    return;
                }

                // Lands transaction details can be split into two lines
                debug("Parsed pending payment land={}, amount={}, movement={}",
                        transaction.landName(),
                        transaction.amount(),
                        transaction.movement());
                pendingPayment = new PendingPayment(
                        transaction.amount(),
                        transaction.movement(),
                        transaction.landName(),
                        System.currentTimeMillis()
                );
                return;
            }

            Double newBalance = parseBalanceLine(line);
            if (newBalance == null || pendingPayment == null) {
                return;
            }

            PaymentData payment = new PaymentData(
                    LocalDate.now(),
                    pendingPayment.amount,
                    pendingPayment.movement,
                    pendingPayment.landName,
                    newBalance
            );
            debug("Resolved pending payment with balance={} for land={}", newBalance, pendingPayment.landName);
            GoogleSheetsService.logPayment(payment);
            pendingPayment = null;
        } catch (Exception exception) {
            ShopLoggerMod.LOGGER.debug("Payment parser error", exception);
            pendingPayment = null;
        }
    }

    private static void clearExpiredPending() {
        if (pendingPayment == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - pendingPayment.timestamp > PENDING_TIMEOUT_MS) {
            pendingPayment = null;
        }
    }

    private static double parseMoney(String rawMoney) {
        return Double.parseDouble(rawMoney.replace(",", ""));
    }

    static ParsedTransaction parseTransactionLine(String line) {
        Matcher transactionMatcher = TRANSACTION_PATTERN.matcher(line);
        if (!transactionMatcher.matches()) {
            return null;
        }

        String action = transactionMatcher.group(1);
        double amount = parseMoney(transactionMatcher.group(2));
        String landName = transactionMatcher.group(3).trim();
        Double newBalance = transactionMatcher.group(4) == null ? null : parseMoney(transactionMatcher.group(4));
        String movement = action.equalsIgnoreCase("put") ? "Deposit" : "Withdraw";
        return new ParsedTransaction(amount, movement, landName, newBalance);
    }

    static Double parseBalanceLine(String line) {
        Matcher balanceMatcher = BALANCE_PATTERN.matcher(line);
        if (!balanceMatcher.matches()) {
            return null;
        }
        return parseMoney(balanceMatcher.group(1));
    }

    record ParsedTransaction(double amount, String movement, String landName, Double newBalance) {
    }

    private record PendingPayment(double amount, String movement, String landName, long timestamp) {
    }

    private static void debug(String message, Object... args) {
        if (ModConfig.get().isDebugLogging()) {
            ShopLoggerMod.LOGGER.info("[ChestLogger Debug] " + message, args);
        }
    }
}
