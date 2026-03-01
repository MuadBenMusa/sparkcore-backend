package com.sparkcore.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IbanUtilsTest {

    @Test
    void testGenerateAndValidateGermanIban() {
        String bankCode = "10050000";
        String accountNumber = "1234567890";

        // Generiere IBAN
        String generatedIban = IbanUtils.generateGermanIban(bankCode, accountNumber);

        // Überprüfungen
        assertNotNull(generatedIban);
        assertTrue(generatedIban.startsWith("DE"));
        assertEquals(22, generatedIban.length(), "Deutsche IBAN muss genau 22 Zeichen lang sein");

        // Prüfsumme validieren
        assertTrue(IbanUtils.isValid(generatedIban), "Generierte IBAN muss eine gültige Prüfsumme aufweisen");
    }

    @Test
    void testIsValid_WithRealValidIban() {
        // Eine zufällig generierte strukturell gültige IBAN für DE
        assertTrue(IbanUtils.isValid("DE89370400440532013000"));
    }

    @Test
    void testIsValid_RejectsInvalidChecksum() {
        // Richtige Länge, aber modifizierte Prüfsumme ("DE88" anstatt "DE89")
        assertFalse(IbanUtils.isValid("DE88370400440532013000"));
    }

    @Test
    void testIsValid_RejectsInvalidLength() {
        // Zu kurz
        assertFalse(IbanUtils.isValid("DE123"));

        // Zu lang
        assertFalse(IbanUtils.isValid("DE893704004405320130001234567890123456"));
    }
}
