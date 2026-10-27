package az.techvibeds.specialservice.dtos.kassa;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KassaIncomeDto {
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private Long accountId;    // Hansı kassaya mədaxil olunur
    private Long categoryId;   // Mədaxil kateqoriyası
    private Long partnerId;    // Kimdən qəbul edirik pulu
}
