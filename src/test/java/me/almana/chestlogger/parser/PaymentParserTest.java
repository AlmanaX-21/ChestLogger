package me.almana.chestlogger.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentParserTest {

    @Test
    void parsesDepositTransactionLine() {
        PaymentParser.ParsedTransaction transaction = PaymentParser.parseTransactionLine(
                "[Lands] You successfully put $30,000.00 in the bank of land Brezg."
        );

        assertNotNull(transaction);
        assertEquals(30000.0D, transaction.amount());
        assertEquals("Deposit", transaction.movement());
        assertEquals("Brezg", transaction.landName());
    }

    @Test
    void parsesBalanceLine() {
        Double balance = PaymentParser.parseBalanceLine("New balance of the bank: $5,109,300.00");

        assertNotNull(balance);
        assertEquals(5109300.0D, balance);
    }
}
