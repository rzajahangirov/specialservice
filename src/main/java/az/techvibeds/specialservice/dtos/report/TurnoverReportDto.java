package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dövriyyə hesabatı — gəlir, xərc, mənfəət/zərər xülasəsi.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TurnoverReportDto {
    private BigDecimal totalSales;          // Ümumi satış dövriyyəsi
    private BigDecimal totalPurchases;      // Ümumi alış dövriyyəsi
    private BigDecimal totalExpenses;       // Ümumi xərclər (dolayı xərclər)
    private BigDecimal totalCashIncome;     // Kassaya mədaxil
    private BigDecimal totalCashExpense;    // Kassadan məxaric
    private BigDecimal grossProfit;         // Ümumi mənfəət (satış - alış)
    private BigDecimal netProfit;           // Xalis mənfəət (ümumi mənfəət - xərclər)
    private BigDecimal turnoverBalance;     // Dövriyyə balansı
}
