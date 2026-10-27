package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Mənfəət / Zərər hesabatı — dövr üzrə xülasə.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfitLossReportDto {
    private BigDecimal totalSalesRevenue;       // Satışdan əldə edilən gəlir
    private BigDecimal totalPurchaseCost;       // Alış xərcləri
    private BigDecimal grossProfit;             // Ümumi mənfəət (satış - alış)
    private BigDecimal totalIndirectExpenses;   // Dolayı xərclər
    private BigDecimal totalCashIncome;         // Kassaya mədaxil
    private BigDecimal totalCashExpense;        // Kassadan məxaric
    private BigDecimal netProfit;               // Xalis mənfəət
    private boolean isProfit;                   // true = mənfəət, false = zərər
}
