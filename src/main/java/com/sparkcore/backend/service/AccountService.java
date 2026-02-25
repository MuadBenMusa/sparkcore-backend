package com.sparkcore.backend.service;

import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.model.Transaction;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.AuditLogRepository;
import com.sparkcore.backend.repository.TransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository; // NEU

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Hilfsmethode: IP-Adresse aus dem aktuellen Request holen
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRemoteAddr() : "UNKNOWN_IP";
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

        auditLogRepository.save(new AuditLog(
                getCurrentUsername(), "CREATE_ACCOUNT",
                "Konto erstellt für: " + ownerName + " (IBAN: " + iban + ")",
                getClientIp(), "SUCCESS"));

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

        auditLogRepository.save(new AuditLog(
                getCurrentUsername(), "TRANSFER",
                "Von: " + fromIban + " Nach: " + toIban + " Betrag: " + amount,
                getClientIp(), "SUCCESS"));
    }

    public java.util.List<Transaction> getTransactionHistory(String iban) {
        return transactionRepository.findBySenderIbanOrReceiverIbanOrderByTimestampDesc(iban, iban);
    }

    public java.util.List<com.sparkcore.backend.model.AuditLog> getAuditLogs() {
        return auditLogRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "timestamp"));
    }

}