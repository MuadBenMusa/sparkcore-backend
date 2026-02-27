package com.sparkcore.backend.service;

import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void getAllAuditLogs_ReturnsSortedList() {
        // --- 1. ARRANGE ---
        AuditLog log1 = new AuditLog("user1", "LOGIN_SUCCESS", "details", "127.0.0.1", "SUCCESS");
        AuditLog log2 = new AuditLog("user2", "LOGIN_FAILED", "details", "127.0.0.1", "FAILURE");

        // Dem Mock beibringen, die Logs zurückzugeben, wenn nach Sortiment gesucht wird
        when(auditLogRepository.findAll(any(Sort.class))).thenReturn(List.of(log1, log2));

        // --- 2. ACT ---
        List<AuditLog> result = auditLogService.getAllAuditLogs();

        // --- 3. ASSERT & VERIFY ---
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());

        // Prüfen, ob das Repository mit der korrekten Sortierung aufgerufen wurde
        verify(auditLogRepository, times(1)).findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}
