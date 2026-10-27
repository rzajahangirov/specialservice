package az.techvibeds.specialservice.dtos.productionOrder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductionOrderExpenseDto {
    private Long id;

    @NotBlank(message = "Expense name is required")
    private String name;

    @NotNull(message = "Expense amount is required")
    private BigDecimal amount;
}
