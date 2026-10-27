package az.techvibeds.specialservice.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payrolls")
public class Payroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private HREmployee employee;

    private YearMonth period; // 2024-07 kimi ay/il formatı

    private BigDecimal baseSalary;   // Əsas maaş (Müqavilədən gələn)
    private BigDecimal bonusAmount;  // Əlavə bonuslar
    private BigDecimal taxAmount;    // Vergilər

    private BigDecimal totalDeductions; // Həmin ay üçün hesablanmış YEKUN kəsilmə məbləği (Deduction cədvəlindən gəlir)

    private BigDecimal advanceAmount; // Verilmiş avans məbləği
    private BigDecimal debtAmount;    // İşçinin şirkətə olan borcu

    private BigDecimal netSalary; // Yekun ödəniş = Base + Bonus - Tax - totalDeductions

    private boolean isPaid;

    @PrePersist
    public void onCreate() {
        calculateNetSalary();
    }

    @PreUpdate
    public void onUpdate() {
        calculateNetSalary();
    }

    private void calculateNetSalary() {
        BigDecimal base = baseSalary != null ? baseSalary : BigDecimal.ZERO;
        BigDecimal bonus = bonusAmount != null ? bonusAmount : BigDecimal.ZERO;
        BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal deductions = totalDeductions != null ? totalDeductions : BigDecimal.ZERO;
        BigDecimal advance = advanceAmount != null ? advanceAmount : BigDecimal.ZERO;

        this.netSalary = base
                .add(bonus)
                .subtract(tax)
                .subtract(deductions)
                .subtract(advance); // Avans əvvəlcədən verilib, maaşdan çıxılır
    }
}
