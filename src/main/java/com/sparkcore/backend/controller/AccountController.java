package com.sparkcore.backend.controller;

import com.sparkcore.backend.dto.CreateAccountRequest;
import com.sparkcore.backend.dto.TransferRequest;
import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.model.Transaction;
import com.sparkcore.backend.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Alles rund um Bankkonten – anlegen, abfragen, überweisen
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // neues Konto anlegen – nur für Bankmitarbeiter (ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account createdAccount = accountService.createAccount(
                request.ownerName(),
                request.initialBalance());
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    // alle Konten abrufen – nur für Bankmitarbeiter
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return new ResponseEntity<>(accountService.getAllAccounts(), HttpStatus.OK);
    }

    // einzelnes Konto anhand der ID – 404 wenn nicht gefunden
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .map(account -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        boolean isAdmin = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                        if (!isAdmin && !account.getOwnerName().equals(auth.getName())) {
                            throw new AccessDeniedException("Zugriff verweigert: Dies ist nicht Ihr Konto.");
                        }
                    }
                    return new ResponseEntity<>(account, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Geldüberweisung – Validierung via @Valid, Logik im Service
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> transferMoney(@Valid @RequestBody TransferRequest request) {
        accountService.transferMoney(
                request.fromIban(),
                request.toIban(),
                request.amount());
        return new ResponseEntity<>("Überweisung erfolgreich verarbeitet.", HttpStatus.OK);
    }

    // Kontoauszug – alle Buchungen dieser IBAN, neueste zuerst
    @GetMapping("/{iban}/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(
            @PathVariable String iban) {
        return new ResponseEntity<>(accountService.getTransactionHistory(iban), HttpStatus.OK);
    }

}
