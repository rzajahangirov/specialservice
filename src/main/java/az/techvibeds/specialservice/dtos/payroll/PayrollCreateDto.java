package az.techvibeds.specialservice.dtos.payroll;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollCreateDto {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Period (Year-Month) is required")
    private YearMonth period;

    @PositiveOrZero(message = "Bonus amount cannot be negative")
    private BigDecimal bonusAmount;

    @PositiveOrZero(message = "Tax amount cannot be negative")
    private BigDecimal taxAmount;
}