package com.sparkcore.backend.service;

import com.sparkcore.backend.dto.TransactionEvent;
import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    /**
     * Reagiert auf eingehende Kafka-Events vom AccountService
     * und speichert den Audit-Log asynchron.
     */
    @KafkaListener(topics = "transaction-events", groupId = "sparkcore-audit-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        String details = "Aktion: " + event.action() + " | Betrag: " + event.amount() +
                " | Von: " + event.fromIban() + " | An: " + event.toIban();

        AuditLog log = new AuditLog(
                event.triggeredByUsername(),
                event.action(),
                details,
                event.clientIp(),
                event.status());

        auditLogRepository.save(log);
        System.out.println("✅ [Kafka Consumer] AuditLog asynchron gespeichert: " + details);
    }
}
