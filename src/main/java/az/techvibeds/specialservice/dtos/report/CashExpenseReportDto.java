package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kassadan məxaric hesabatı — xərc tranzaksiyaları.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashExpenseReportDto {
    private Long transactionId;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private String categoryName;
    private String accountName;
    private String partnerName;        // Partnyorun adı (ödəniş edən/alan)
}
