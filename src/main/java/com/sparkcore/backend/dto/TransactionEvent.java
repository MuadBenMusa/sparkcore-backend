package com.sparkcore.backend.dto;

import java.math.BigDecimal;

/**
 * Event-DTO, das vom AccountService an Kafka gesendet wird, sobald eine
 * Buchung (Transfer) oder Kontoerstellung stattfindet.
 * Der Consumer (AuditLogService) liest dieses Event und schreibt es in die DB.
 */
public record TransactionEvent(
        String action, // z.B. "TRANSFER" oder "CREATE_ACCOUNT"
        String fromIban,
        String toIban,
        BigDecimal amount,
        String status,
        String triggeredByUsername,
        String clientIp) {
}
