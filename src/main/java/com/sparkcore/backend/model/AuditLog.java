package com.sparkcore.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String action;
    private String details;
    private String ipAddress;
    private String status;
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(String username, String action, String details, String ipAddress, String status) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // --- Getter (Setter weglassen für Immutability, genau wie bei den Transactions) ---
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
    public String getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
}