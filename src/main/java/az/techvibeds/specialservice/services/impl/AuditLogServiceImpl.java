package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.auditLog.AuditLogResponse;
import az.techvibeds.specialservice.models.AuditLog;
import az.techvibeds.specialservice.repositories.AuditLogRepository;
import az.techvibeds.specialservice.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void logAction(String actorEmail, String action, String target, String details) {
        AuditLog logEntry = AuditLog.builder()
                .actorEmail(actorEmail)
                .action(action)
                .target(target)
                .details(details)
                .timestamp(LocalDateTime.now())
                // IP ünvanı üçün HttpServletRequest-dən istifadə edə bilərsən
                .build();
        auditLogRepository.save(logEntry);
        log.debug("Audit log saved: {} by {}", action, actorEmail);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String adminEmail, Pageable pageable) {
        log.info("Fetching audit logs for dashboard. Requested by: {}", adminEmail);

        return auditLogRepository.findAll(pageable)
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .timestamp(log.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .actor(log.getActorEmail())
                        .event(log.getAction())
                        .ipAddress(log.getIpAddress())
                        .build());
    }
}
