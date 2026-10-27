package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.DeductionDurationType;
import az.techvibeds.specialservice.enums.DeductionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "deductions")
public class Deduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HREmployee employee;

    @Enumerated(EnumType.STRING)
    private DeductionType type; // Kəsilmənin növü

    private BigDecimal amount; // Aylıq kəsiləcək məbləğ
    private String reason;// Səbəb

    private LocalDate startDate; // Tətbiq olunmağa başladığı tarix
    private Integer duration; // Kəsilmənin tətbiq olunacağı müddət

    @Enumerated(EnumType.STRING)
    private DeductionDurationType durationType; // Müddətin növü (Aylarla və ya Günlərlə)

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    private boolean isActive; // Kəsilmə hələ də davam edirmi?
}
