package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.auditLog.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {
    void logAction(String actorEmail, String action, String target, String details);

    Page<AuditLogResponse> getAuditLogs(String adminEmail, Pageable pageable);
}
