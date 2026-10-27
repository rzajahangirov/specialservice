package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Aylıq gəlir/xərc xülasəsi — şəkildəki qrafik üçün
 */
@Data
public class MonthlySummaryDto {
    private String month;          // "Yanvar", "Fevral" ...
    private BigDecimal income;     // O aydakı ümumi gəlir
    private BigDecimal expense;    // O aydakı ümumi xərc
}
