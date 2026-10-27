package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kassaya mədaxil hesabatı — gəlir tranzaksiyaları.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashIncomeReportDto {
    private Long transactionId;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private String categoryName;       // Tranzaksiya kateqoriyası
    private String accountName;        // Hesab adı (məs: Kassa, Bank Hesabı)
    private String partnerName;        // Partnyorun adı (ödəniş edən/alan)
}
