package az.techvibeds.specialservice.dtos.kassa;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class KassaTransferDto {
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private Long fromAccountId; // Hansı kassadan məxaric olunur
    private Long toAccountId;   // Hansı kassaya mədaxil olunur
}
