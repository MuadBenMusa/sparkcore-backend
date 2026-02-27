package com.sparkcore.backend.service;

import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.AuditLogRepository;
import com.sparkcore.backend.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// Sagt JUnit, dass wir Mockito für die "Fake-Datenbanken" nutzen wollen
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository; // Fake-Datenbank 1

    @Mock
    private TransactionRepository transactionRepository; // Fake-Datenbank 2

    @Mock
    private AuditLogRepository auditLogRepository; // Fake-Datenbank 3 (required by AccountService)

    @InjectMocks
    private AccountService accountService; // Unser ECHTER Service, in den die Fakes reingesteckt werden

    @Test
    void transferMoney_ThrowsException_WhenInsufficientFunds() {
        // --- 1. ARRANGE (Vorbereitung) ---
        String senderIban = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("500.00");

        // Sender hat nur 100€ (zu wenig für die 500€ Überweisung!)
        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setBalance(new BigDecimal("100.00"));

        // Empfänger hat 0€
        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("0.00"));

        // Dem Mock-Repository beibringen, wie es antworten soll, wenn der Service nach
        // den Konten sucht
        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        // --- 2. ACT & ASSERT (Ausführen & Prüfen) ---
        // Wir ERWARTEN, dass eine IllegalArgumentException geworfen wird, wenn wir die
        // Methode aufrufen
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferMoney(senderIban, receiverIban, transferAmount));

        // --- 3. VERIFY (Sicherstellen, dass alles exakt nach Plan lief) ---
        // Prüfen, ob der Fehlertext genau übereinstimmt (damit fängt uns der Test,
        // falls jemand den Text ändert)
        assertEquals("Nicht ausreichendes Guthaben auf dem Sender-Konto.", exception.getMessage());

        // GANZ WICHTIG FÜR BANKEN: Wir prüfen hier, ob das Repository JEMALS die
        // save-Methode aufgerufen hat.
        // Da der Kontostand zu niedrig war, darf auf gar keinen Fall etwas gespeichert
        // worden sein!
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferMoney_Success() {
        // --- 1. ARRANGE (Vorbereitung) ---
        String senderIban = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("100.00");

        // Sender hat 500€
        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setBalance(new BigDecimal("500.00"));

        // Empfänger hat 200€
        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("200.00"));

        // Dem Mock beibringen, unsere präparierten Konten zurückzugeben
        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        // --- 2. ACT (Ausführen) ---
        // Diesmal erwarten wir KEINE Exception, wir rufen die Methode einfach auf
        accountService.transferMoney(senderIban, receiverIban, transferAmount);

        // --- 3. ASSERT & VERIFY (Prüfen) ---
        // 3a: Hat der Service im Speicher richtig gerechnet?
        assertEquals(new BigDecimal("400.00"), senderAccount.getBalance()); // 500 - 100 = 400
        assertEquals(new BigDecimal("300.00"), receiverAccount.getBalance()); // 200 + 100 = 300

        // 3b: Hat der Service die geänderten Konten an die Datenbank zum Speichern
        // übergeben?
        // times(1) bedeutet: Genau einmal pro Konto aufgerufen!
        verify(accountRepository, times(1)).save(senderAccount);
        verify(accountRepository, times(1)).save(receiverAccount);

        // 3c: Wurde ein Eintrag ins Audit-Log geschrieben?
        verify(transactionRepository, times(1)).save(any(com.sparkcore.backend.model.Transaction.class));
    }

}