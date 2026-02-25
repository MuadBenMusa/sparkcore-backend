package com.sparkcore.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderIban;

    @Column(nullable = false)
    private String receiverIban;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status; // z.B. "SUCCESS" oder "FAILED"

    // --- Standard Konstruktor ---
    public Transaction() {
    }

    // --- Bequemer Konstruktor für unseren Service ---
    public Transaction(String senderIban, String receiverIban, BigDecimal amount, String status) {
        this.senderIban = senderIban;
        this.receiverIban = receiverIban;
        this.amount = amount;
        this.timestamp = LocalDateTime.now(); // Speichert automatisch die genaue Uhrzeit
        this.status = status;
    }

    // --- Getter (Setter brauchen wir hier kaum, da Transaktionen im Nachhinein nicht verändert werden dürfen!) ---
    public Long getId() { return id; }
    public String getSenderIban() { return senderIban; }
    public String getReceiverIban() { return receiverIban; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}