package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Xərc hesabatı — dolayı xərclərin detallı siyahısı.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseReportDto {
    private Long id;
    private String name;              // Xərcin adı
    private String category;          // Xərc kateqoriyası
    private BigDecimal amount;        // Məbləğ
    private String currency;          // Valyuta
    private Date expenseDate;         // Xərc tarixi
}
