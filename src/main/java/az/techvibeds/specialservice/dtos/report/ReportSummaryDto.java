package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Bütün hesabatların ümumi wrapper DTO-su.
 * Hər bir hesabat növü ayrıca endpoint-dən qaytarılır,
 * lakin ehtiyac olsa hamısını bir yerdə də almaq olar.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportSummaryDto {
    private String companyName;
    private String reportPeriod;               // "2026-01-01 — 2026-05-07"
    private String generatedAt;                // Hesabatın yaradılma vaxtı

    // Xülasə rəqəmlər
    private BigDecimal totalSales;
    private BigDecimal totalPurchases;
    private BigDecimal totalExpenses;
    private BigDecimal totalCashIncome;
    private BigDecimal totalCashExpense;
    private BigDecimal netProfit;
    private boolean isProfit;

    // Detallı siyahılar
    private List<ProductMovementDto> productMovements;
    private List<SalesReportDto> salesReport;
    private List<PurchaseReportDto> purchaseReport;
    private TurnoverReportDto turnoverReport;
    private List<ExpenseReportDto> expenseReport;
    private List<ManufacturedProductReportDto> manufacturedProducts;
    private ProfitLossReportDto profitLossReport;
    private List<CashIncomeReportDto> cashIncomeReport;
    private List<CashExpenseReportDto> cashExpenseReport;

    // Tikinti və Xidmət Hesabatları
    private List<ConstructionReportDto> constructionReport;
    private List<ServiceReportDto> serviceReport;

    /**
     * Şirkətin aktiv modullarının backendKey set-i.
     * Frontend bu sahəyə baxaraq hansı hesabat bölmələrini göstərəcəyini bilir.
     * Məsələn: ["FINANCE", "INVOICE", "INVENTORY", "CONSTRUCTION"]
     */
    private Set<String> accessibleModules;
}

