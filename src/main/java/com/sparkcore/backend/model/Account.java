package com.sparkcore.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Ein Bankkonto – wird in der Tabelle "accounts" gespeichert
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String iban; // keine zwei Konten mit derselben IBAN

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private BigDecimal balance; // BigDecimal statt double – bei Geld immer genau!

    /**
     * Optimistic Locking – verhindert Double-Spend bei gleichzeitigen Überweisungen.
     * Wenn zwei Transaktionen dieselbe Version lesen und beide speichern wollen,
     * wirft JPA eine OptimisticLockException für die zweite → kein stiller Datenverlust.
     */
    @Version
    private Long version;

    public Account() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getVersion() {
        return version;
    }
}
