package az.techvibeds.specialservice.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryDto {
    private BigDecimal totalRevenue;
    private String revenueTrend; // e.g. "+15% keçən ay"
    
    private BigDecimal totalExpense;
    private String expenseTrend; // e.g. "-5% keçən ay"
    
    private BigDecimal netProfit;
    private String profitTrend; // e.g. "+20% keçən ay"
    
    private BigDecimal expectedPayments;
    private String expectedPaymentsInfo; // e.g. "30 gün ərzində"
}
