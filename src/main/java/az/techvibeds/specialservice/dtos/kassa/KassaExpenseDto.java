package az.techvibeds.specialservice.dtos.kassa;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KassaExpenseDto {
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private Long accountId;    // Hansı kassadan məxaric olunur
    private Long categoryId;   // Məxaric kateqoriyası (xərc növü)
    private Long partnerId;    // Kimə ödəyirik pulu
}
