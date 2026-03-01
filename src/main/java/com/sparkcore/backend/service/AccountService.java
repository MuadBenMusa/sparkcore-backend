package com.sparkcore.backend.service;

import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.model.Transaction;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.TransactionRepository;
import com.sparkcore.backend.dto.TransactionEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.sparkcore.backend.util.RequestUtils;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            KafkaTemplate<Object, Object> kafkaTemplate) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    public Account createAccount(String ownerName, String iban, BigDecimal initialBalance) {
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
    public void transferMoney(String fromIban, String toIban, BigDecimal amount) {
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
                "TRANSFER",
                fromIban,
                toIban,
                amount,
                "SUCCESS",
                getCurrentUsername(),
                RequestUtils.getClientIp());
        kafkaTemplate.send("transaction-events", event);
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