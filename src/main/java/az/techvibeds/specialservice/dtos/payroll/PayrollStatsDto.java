package az.techvibeds.specialservice.dtos.payroll;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollStatsDto {
    private BigDecimal totalExpense;
    private BigDecimal averageSalary;
    private Long recordCount;
}
