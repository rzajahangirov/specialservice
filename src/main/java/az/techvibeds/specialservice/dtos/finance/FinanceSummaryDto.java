package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard üçün maliyyə xülasəsi — şəkildəki 4 kartın və qrafik datanın qaytarılması
 */
@Data
public class FinanceSummaryDto {
    private BigDecimal totalIncome;           // Ümumi Gəlir
    private BigDecimal totalExpense;          // Ümumi Xərc
    private BigDecimal currentBalance;        // Cari Balans
    private BigDecimal netProfit;             // Xalis Mənfəət

    private BigDecimal incomeChangePercent;   // Əvvəlki dövrə nisbətən Gəlir dəyişikliyi %
    private BigDecimal expenseChangePercent;  // Əvvəlki dövrə nisbətən Xərc dəyişikliyi %
    private BigDecimal balanceChangePercent;  // Əvvəlki dövrə nisbətən Balans dəyişikliyi %
    private BigDecimal profitChangePercent;   // Əvvəlki dövrə nisbətən Mənfəət dəyişikliyi %

    private List<MonthlySummaryDto> monthlySummaries; // Qrafik üçün aylıq data
}
