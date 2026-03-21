package com.sparkcore.backend.service;

import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.model.IdempotencyKey;
import com.sparkcore.backend.model.Transaction;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.IdempotencyKeyRepository;
import com.sparkcore.backend.repository.TransactionRepository;
import com.sparkcore.backend.dto.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.sparkcore.backend.util.IbanUtils;
import com.sparkcore.backend.util.RequestUtils;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            KafkaTemplate<Object, Object> kafkaTemplate, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    public Account createAccount(String ownerName, BigDecimal initialBalance) {
        // Generate a random 10-digit account number
        String accountNumber = String.format("%010d", new Random().nextInt(1000000000));
        // SparkCore Bank Code (Simulated)
        String bankCode = "10050000";

        String iban = IbanUtils.generateGermanIban(bankCode, accountNumber);

        Account newAccount = new Account();
        newAccount.setOwnerName(ownerName);
        newAccount.setIban(iban);
        newAccount.setBalance(initialBalance);

        Account savedAccount = accountRepository.save(newAccount);

        // Async: Event an Kafka senden (Entkopplung)
        TransactionEvent event = new TransactionEvent(
                "CREATE_ACCOUNT",
                null,
                iban,
                initialBalance,
                "SUCCESS",
                getCurrentUsername(),
                RequestUtils.getClientIp());
        kafkaTemplate.send("transaction-events", event);

        return savedAccount;
    }

    public java.util.List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public java.util.Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public String transferMoney(String fromIban, String toIban, BigDecimal amount,
                                String idempotencyKey, String username) {

        // --- IDEMPOTENCY CHECK ---
        // Falls der Client denselben Key nochmal sendet (z.B. nach einem Network-Timeout),
        // geben wir die gecachte Antwort zurück, ohne die Überweisung nochmal auszuführen.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = idempotencyKeyRepository.findByIdempotencyKeyAndUsername(idempotencyKey, username);
            if (cached.isPresent()) {
                return cached.get().getResponseBody(); // Identische Antwort, keine zweite Buchung!
            }
        }

        // negativer Betrag macht keinen Sinn
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Überweisungsbetrag muss größer als 0 sein.");
        }

        // Eigenüberweisung verhindern
        if (fromIban.equals(toIban)) {
            throw new IllegalArgumentException("Sender- und Empfängerkonto dürfen nicht identisch sein.");
        }

        // Sender- und Empfängerkonto aus der DB laden
        Account fromAccount = accountRepository.findByIban(fromIban)
                .orElseThrow(() -> new IllegalArgumentException("Sender-Konto nicht gefunden: " + fromIban));

        Account toAccount = accountRepository.findByIban(toIban)
                .orElseThrow(() -> new IllegalArgumentException("Empfänger-Konto nicht gefunden: " + toIban));

        // Deckungsprüfung
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Nicht ausreichendes Guthaben auf dem Sender-Konto.");
        }

        // Kontostand anpassen und speichern
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Buchung ins Transaktionslog schreiben
        Transaction successfulTransaction = new Transaction(fromIban, toIban, amount, "SUCCESS");
        transactionRepository.save(successfulTransaction);

        // Async: Event an Kafka senden (Entkopplung)
        TransactionEvent event = new TransactionEvent(
                "TRANSFER", fromIban, toIban, amount, "SUCCESS",
                getCurrentUsername(), RequestUtils.getClientIp());
        kafkaTemplate.send("transaction-events", event);

        String responseMessage = "Überweisung erfolgreich verarbeitet.";

        // --- IDEMPOTENCY KEY SPEICHERN ---
        // Nur speichern wenn ein Key mitgeschickt wurde.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeyRepository.save(
                    new IdempotencyKey(idempotencyKey, username, responseMessage, 200));
        }

        return responseMessage;
    }

    public java.util.List<Transaction> getTransactionHistory(String iban) {
        // Ownership Check: Darf der aktuelle Nutzer dieses Konto einsehen?
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Account account = accountRepository.findByIban(iban)
                    .orElseThrow(() -> new IllegalArgumentException("Konto nicht gefunden: " + iban));

            if (!account.getOwnerName().equals(auth.getName())) {
                throw new AccessDeniedException("Zugriff verweigert: Dies ist nicht Ihr Konto.");
            }
        }

        return transactionRepository.findBySenderIbanOrReceiverIbanOrderByTimestampDesc(iban, iban);
    }

}