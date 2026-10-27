package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionReadDto {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String type;
    private String description;
    private String accountName;
    private String categoryName;
    private String status; // Tamamlandı, Gözləmədə
    private Long partnerId;
    private String partnerName;
    private Long invoiceId;
    private String invoiceNumber;
}
