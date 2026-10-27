package az.techvibeds.specialservice.dtos.indirectExpense;

import az.techvibeds.specialservice.enums.CurrencyType;
import az.techvibeds.specialservice.enums.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IndirectExpenseCreateDto {
    @NotBlank(message = "Expense name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private CurrencyType currency;

    @NotNull(message = "Expense date is required")
    private Date expenseDate;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;
}
