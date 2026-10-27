package az.techvibeds.specialservice.controllers.roleAndSecurity;

import az.techvibeds.specialservice.dtos.auditLog.AuditLogResponse;
import az.techvibeds.specialservice.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/security/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('USER_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllLogs(
            Principal principal,
            @org.springdoc.core.annotations.ParameterObject 
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(auditLogService.getAuditLogs(principal.getName(), pageable));
    }
}
