package com.sparkcore.backend.service;

import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
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
}
