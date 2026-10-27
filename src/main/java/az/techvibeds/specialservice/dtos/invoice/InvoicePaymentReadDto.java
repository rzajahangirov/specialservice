package az.techvibeds.specialservice.dtos.invoice;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoicePaymentReadDto {
    private Long transactionId;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String accountName;          // Hansı kassaya/hesaba ödənildi
    private Long accountId;
    private String description;
    private String createdByName;        // Kim tərəfindən ödənildi
}
