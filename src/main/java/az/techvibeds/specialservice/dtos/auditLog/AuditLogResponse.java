package az.techvibeds.specialservice.dtos.auditLog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String timestamp;
    private String actor;
    private String event;
    private String ipAddress;
}
