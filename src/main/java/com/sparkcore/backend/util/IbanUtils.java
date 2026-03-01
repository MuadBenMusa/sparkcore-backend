package com.sparkcore.backend.util;

import java.math.BigInteger;

/**
 * Utility-Klasse für die Generierung und Validierung von IBANs
 * basierend auf dem ISO 13616 Standard (Modulo-97 Checksumme / ISO 7064).
 */
public final class IbanUtils {

    private IbanUtils() {
        // Utils-Klasse soll nicht instanziiert werden
    }

    /**
     * Generiert eine mathematisch gültige deutsche IBAN.
     * 
     * @param bankCode      Die 8-stellige Bankleitzahl (BLZ)
     * @param accountNumber Die bis zu 10-stellige Kontonummer
     * @return Die gültige IBAN im Format DExxBBBBBBBBCCCCCCCCCC
     */
    public static String generateGermanIban(String bankCode, String accountNumber) {
        if (bankCode == null || !bankCode.matches("\\d{8}")) {
            throw new IllegalArgumentException("Bankleitzahl muss genau 8 Ziffern lang sein.");
        }
        if (accountNumber == null || !accountNumber.matches("\\d{1,10}")) {
            throw new IllegalArgumentException("Kontonummer muss zwischen 1 und 10 Ziffern lang sein.");
        }

        // Kontonummer auf genau 10 Stellen mit führenden Nullen auffüllen
        // (Right-Padding by formatting)
        String paddedAccountNumber = String.format("%010d", Long.parseLong(accountNumber));

        // 1. IBAN-Bestandteile zusammensetzen (Bankleitzahl + Kontonummer)
        String bban = bankCode + paddedAccountNumber;

        // 2. Ländercode (DE) ans Ende verschieben und in Zahlen umwandeln (A=10, B=11
        // ... D=13, E=14)
        // Länder-Identifikator für DE = "1314"
        // Platzhalter für die zweistellige Prüfsumme = "00"
        String numericIban = bban + "131400";

        // 3. Modulo 97 berechnen (ISO 7064)
        BigInteger ibanNumber = new BigInteger(numericIban);
        int checksum = 98 - ibanNumber.remainder(BigInteger.valueOf(97)).intValue();

        // 4. Prüfsumme immer zweistellig formatieren (01 ... 97)
        String checkDigits = String.format("%02d", checksum);

        String result = "DE" + checkDigits + bban;

        // Selbst-Check: Schützt vor Logikfehlern in der Berechnung oben
        if (!isValid(result)) {
            throw new IllegalStateException("Generierte IBAN ist mathematisch ungültig: " + result);
        }

        return result;
    }

    /**
     * Validiert eine IBAN auf strukturelle und mathematische Korrektheit.
     * 
     * @param iban Die zu prüfende IBAN (darf Leerzeichen enthalten)
     * @return true, wenn die IBAN laut ISO 13616 gültig ist.
     */
    public static boolean isValid(String iban) {
        if (iban == null || iban.isBlank()) {
            return false;
        }

        // Alle Leerzeichen entfernen und Großschreibung erzwingen
        String cleanIban = iban.replaceAll("\\s+", "").toUpperCase();

        // Einfacher Längen-Check (Deutsche IBAN hat exakt 22 Stellen, max weltweit 34)
        if (cleanIban.length() < 15 || cleanIban.length() > 34) {
            return false;
        }

        // Länder- und Prüfziffern ans Ende verschieben (Stellen 1-4 hinten anhängen)
        String rearrangedIban = cleanIban.substring(4) + cleanIban.substring(0, 4);

        // Buchstaben durch Zahlen ersetzen (A=10, B=11, ... Z=35)
        StringBuilder numericIban = new StringBuilder();
        for (int i = 0; i < rearrangedIban.length(); i++) {
            char c = rearrangedIban.charAt(i);
            if (Character.isDigit(c)) {
                numericIban.append(c);
            } else if (Character.isLetter(c)) {
                int numericValue = Character.getNumericValue(c);
                numericIban.append(numericValue);
            } else {
                return false; // Ungültiges Zeichen gefunden
            }
        }

        // ISO 7064 Modulo-97 Prüfung
        try {
            BigInteger ibanNumber = new BigInteger(numericIban.toString());
            return ibanNumber.remainder(BigInteger.valueOf(97)).intValue() == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
