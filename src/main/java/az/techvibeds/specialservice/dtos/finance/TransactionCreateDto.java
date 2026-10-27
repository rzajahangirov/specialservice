package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionCreateDto {
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String type; // INCOME or EXPENSE
    private String description;
    private Long accountId;
    private Long categoryId;
}
