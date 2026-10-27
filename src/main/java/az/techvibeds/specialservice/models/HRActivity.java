package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.ActivityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "hr_activities")
public class HRActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private HREmployee employee;

    private String activityType;

    @CreationTimestamp // Hibernate tərəfindən avtomatik yaradılma saatı yazılır
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ActivityStatus status;
}
