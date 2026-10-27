package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.finance.FinanceSummaryDto;
import az.techvibeds.specialservice.dtos.report.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Hesabatlar modulu — bütün hesabat növləri üçün service interface.
 * Hər metod şirkətə aid məlumatları tarix aralığı ilə filtrləyir.
 */
public interface ReportService {

    // ——— Mövcud metodlar (köhnə uyğunluq üçün saxlanılır) ———
    FinanceSummaryDto getBalanceSheet(String userEmail);
    FinanceSummaryDto getProfitLoss(String userEmail);
    FinanceSummaryDto getCashFlow(String userEmail);
    byte[] exportExcel(String userEmail);
    byte[] exportPdf(String userEmail);

    // ——— Yeni Hesabat metodları ———

    /** 1. Malın hərəkəti hesabatı */
    List<ProductMovementDto> getProductMovementReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 1.1 Detallı malın hərəkəti hesabatı */
    List<ProductMovementDetailReportDto> getProductMovementDetailsReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 2. Satış hesabatı */
    List<SalesReportDto> getSalesReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 2.1 Detallı Satış hesabatı */
    List<DetailedSalesReportDto> getDetailedSalesReport(
            String userEmail, LocalDate startDate, LocalDate endDate, java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount);

    /** 3. Alış hesabatı */
    List<PurchaseReportDto> getPurchaseReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 3.1 Detallı Alış hesabatı */
    List<DetailedPurchaseReportDto> getDetailedPurchaseReport(
            String userEmail, LocalDate startDate, LocalDate endDate, java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount);

    /** 4. Dövriyyə hesabatı */
    TurnoverReportDto getTurnoverReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 5. Xərc hesabatı */
    List<ExpenseReportDto> getExpenseReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 6. İstehsal olunan mal (məhsul) hesabatı */
    List<ManufacturedProductReportDto> getManufacturedProductReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 7. Mənfəət / Zərər hesabatı */
    ProfitLossReportDto getProfitLossReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 8. Kassaya mədaxil hesabatı */
    List<CashIncomeReportDto> getCashIncomeReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 9. Kassadan məxaric hesabatı */
    List<CashExpenseReportDto> getCashExpenseReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 10. Bütün hesabatların ümumi xülasəsi */
    ReportSummaryDto getFullReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 11. Tikinti layihələri hesabatı */
    List<ConstructionReportDto> getConstructionReport(String userEmail, LocalDate startDate, LocalDate endDate);

    /** 12. Xidmətlər hesabatı */
    List<ServiceReportDto> getServiceReport(String userEmail, LocalDate startDate, LocalDate endDate);
}
