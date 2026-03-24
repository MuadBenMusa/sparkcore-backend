package com.sparkcore.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.model.OutboxEvent;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.IdempotencyKeyRepository;
import com.sparkcore.backend.repository.OutboxEventRepository;
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
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    // Real ObjectMapper — no need to mock the serialization itself
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Manuell Instanz bauen, weil ObjectMapper kein Mock ist und @InjectMocks
    // dann nicht sauber geht
    @InjectMocks
    private AccountService accountService;

    /**
     * @InjectMocks doesn't inject the non-mock ObjectMapper, so we use a helper to create
     * the sut manually with the real ObjectMapper.
     */
    private AccountService buildService() {
        return new AccountService(
                accountRepository,
                transactionRepository,
                outboxEventRepository,
                idempotencyKeyRepository,
                objectMapper);
    }

    @Test
    void transferMoney_ThrowsException_WhenInsufficientFunds() {
        AccountService sut = buildService();

        String senderIban   = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("500.00");

        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setBalance(new BigDecimal("100.00")); // Zu wenig!

        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("0.00"));

        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sut.transferMoney(senderIban, receiverIban, transferAmount, null, "testuser"));

        assertEquals("Nicht ausreichendes Guthaben auf dem Sender-Konto.", exception.getMessage());

        // GANZ WICHTIG: Nichts darf gespeichert worden sein
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void transferMoney_Success() {
        AccountService sut = buildService();

        String senderIban   = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("100.00");

        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setBalance(new BigDecimal("500.00"));

        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        sut.transferMoney(senderIban, receiverIban, transferAmount, null, "testuser");

        // Kontostände korrekt angepasst?
        assertEquals(new BigDecimal("400.00"), senderAccount.getBalance()); // 500 - 100
        assertEquals(new BigDecimal("300.00"), receiverAccount.getBalance()); // 200 + 100

        // Konten gespeichert?
        verify(accountRepository, times(1)).save(senderAccount);
        verify(accountRepository, times(1)).save(receiverAccount);

        // Outbox-Event wurde gespeichert (statt direktem Kafka-Send)?
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    // -----------------------------------------------------------------------
    // Idempotency Tests
    // -----------------------------------------------------------------------

    @Test
    void transferMoney_ReturnsCachedResponse_WhenIdempotencyKeyAlreadyExists() {
        AccountService sut = buildService();

        String senderIban     = "DE_SENDER";
        String receiverIban   = "DE_RECEIVER";
        BigDecimal amount     = new BigDecimal("75.00");
        String idempotencyKey = "unique-key-abc-123";
        String username       = "max_muster";
        String cachedResponse = "Überweisung erfolgreich verarbeitet.";

        com.sparkcore.backend.model.IdempotencyKey existingKey =
                new com.sparkcore.backend.model.IdempotencyKey(
                        idempotencyKey, username, cachedResponse, 200);
        when(idempotencyKeyRepository.findByIdempotencyKeyAndUsername(idempotencyKey, username))
                .thenReturn(Optional.of(existingKey));

        String result = sut.transferMoney(senderIban, receiverIban, amount, idempotencyKey, username);

        assertEquals(cachedResponse, result);

        // KRITISCH: Kein Konto geladen, kein Save, kein Outbox-Event
        verify(accountRepository, never()).findByIban(anyString());
        verify(accountRepository, never()).save(any(Account.class));
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    // -----------------------------------------------------------------------
    // Domain Validation Tests
    // -----------------------------------------------------------------------

    @Test
    void transferMoney_ThrowsException_WhenSenderAndReceiverAreTheSameIban() {
        AccountService sut = buildService();

        String iban   = "DE46100500000284667551";
        BigDecimal amount = new BigDecimal("10.00");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sut.transferMoney(iban, iban, amount, null, "testuser"));

        assertEquals("Sender- und Empfängerkonto dürfen nicht identisch sein.", exception.getMessage());

        verify(accountRepository, never()).findByIban(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferMoney_ThrowsException_WhenAmountIsNegative() {
        AccountService sut = buildService();

        String senderIban    = "DE_SENDER";
        String receiverIban  = "DE_RECEIVER";
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sut.transferMoney(senderIban, receiverIban, negativeAmount, null, "testuser"));

        assertEquals("Überweisungsbetrag muss größer als 0 sein.", exception.getMessage());

        verify(accountRepository, never()).findByIban(anyString());
        verify(accountRepository, never()).save(any());
    }
}