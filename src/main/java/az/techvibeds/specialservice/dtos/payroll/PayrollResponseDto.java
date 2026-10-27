package az.techvibeds.specialservice.dtos.payroll;

import lombok.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollResponseDto {
    private Long id;
    private Long employeeId;
    private String employeeFullName;
    private YearMonth period;
    private BigDecimal baseSalary;
    private BigDecimal bonusAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalDeductions;
    private BigDecimal advanceAmount;   // Verilmiş avans
    private BigDecimal debtAmount;      // İşçinin borcu
    private BigDecimal netSalary;
    private boolean isPaid;

    /** Frontend üçün: işçinin borcu varsa true */
    public boolean isInDebt() {
        return debtAmount != null && debtAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Frontend üçün: avans varsa true */
    public boolean hasAdvance() {
        return advanceAmount != null && advanceAmount.compareTo(BigDecimal.ZERO) > 0;
    }


}