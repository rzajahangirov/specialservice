package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String actorEmail;
    private String action;
    private String target;
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime timestamp;
}
