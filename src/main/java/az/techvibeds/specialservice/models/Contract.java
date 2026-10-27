package az.techvibeds.specialservice.models;

import az.techvibeds.specialservice.enums.ContractType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HREmployee employee;

    @Enumerated(EnumType.STRING)
    private ContractType contractType;

    private BigDecimal salary; // işçinin aylıq maaşı

    private LocalDate startDate;
    private LocalDate endDate;

    private String documentUrl;
    private boolean isActive;
}
