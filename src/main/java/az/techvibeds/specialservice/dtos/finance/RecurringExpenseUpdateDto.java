package az.techvibeds.specialservice.dtos.finance;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringExpenseUpdateDto {
    private String name;

    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal amount;

    private String description;

    private String frequency;

    private LocalDate endDate;

    private Long accountId;

    private Long categoryId;
}
