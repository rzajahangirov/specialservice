package az.techvibeds.specialservice.controllers.report;

import az.techvibeds.specialservice.dtos.finance.FinanceSummaryDto;
import az.techvibeds.specialservice.dtos.report.*;
import az.techvibeds.specialservice.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Hesabatlar Controller — bütün hesabat endpointləri.
 * Hər endpoint tarix aralığı (startDate, endDate) qəbul edir.
 * Tarix göndərilməzsə, cari ilin əvvəlindən bu günə qədər olan dövr götürülür.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    // ——— Mövcud endpointlər (geriyə uyğunluq) ———

    @GetMapping("/balance-sheet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceSummaryDto> getBalanceSheet(Principal principal) {
        return ResponseEntity.ok(reportService.getBalanceSheet(principal.getName()));
    }

    @GetMapping("/profit-loss")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceSummaryDto> getProfitLoss(Principal principal) {
        return ResponseEntity.ok(reportService.getProfitLoss(principal.getName()));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceSummaryDto> getCashFlow(Principal principal) {
        return ResponseEntity.ok(reportService.getCashFlow(principal.getName()));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportExcel(Principal principal) {
        byte[] excelBytes = reportService.exportExcel(principal.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance-report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPdf(Principal principal) {
        byte[] pdfBytes = reportService.exportPdf(principal.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=finance-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ═══════════════════════════════════════════════════════════════
    //  YENİ HESABAT ENDPOINTLƏRİ
    // ═══════════════════════════════════════════════════════════════

    /** 1. Malın hərəkəti hesabatı */
    @GetMapping("/product-movement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductMovementDto>> getProductMovementReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getProductMovementReport(principal.getName(), dates[0], dates[1]));
    }

    /** 1.1 Detallı malın hərəkəti hesabatı */
    @GetMapping("/product-movement/details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductMovementDetailReportDto>> getProductMovementDetailsReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getProductMovementDetailsReport(principal.getName(), dates[0], dates[1]));
    }

    /** 2. Satış hesabatı */
    @GetMapping("/sales")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalesReportDto>> getSalesReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getSalesReport(principal.getName(), dates[0], dates[1]));
    }

    /** 2.1 Detallı Satış hesabatı */
    @GetMapping("/sales/detailed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DetailedSalesReportDto>> getDetailedSalesReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getDetailedSalesReport(principal.getName(), dates[0], dates[1], minAmount, maxAmount));
    }

    /** 3. Alış hesabatı */
    @GetMapping("/purchases")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PurchaseReportDto>> getPurchaseReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getPurchaseReport(principal.getName(), dates[0], dates[1]));
    }

    /** 3.1 Detallı Alış hesabatı */
    @GetMapping("/purchases/detailed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DetailedPurchaseReportDto>> getDetailedPurchaseReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getDetailedPurchaseReport(principal.getName(), dates[0], dates[1], minAmount, maxAmount));
    }

    /** 4. Dövriyyə hesabatı */
    @GetMapping("/turnover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TurnoverReportDto> getTurnoverReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getTurnoverReport(principal.getName(), dates[0], dates[1]));
    }

    /** 5. Xərc hesabatı */
    @GetMapping("/expenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExpenseReportDto>> getExpenseReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getExpenseReport(principal.getName(), dates[0], dates[1]));
    }

    /** 6. İstehsal olunan mal hesabatı */
    @GetMapping("/manufactured-products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ManufacturedProductReportDto>> getManufacturedProductReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getManufacturedProductReport(principal.getName(), dates[0], dates[1]));
    }

    /** 7. Mənfəət / Zərər hesabatı */
    @GetMapping("/profit-loss-report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfitLossReportDto> getProfitLossReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getProfitLossReport(principal.getName(), dates[0], dates[1]));
    }

    /** 8. Kassaya mədaxil hesabatı */
    @GetMapping("/cash-income")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CashIncomeReportDto>> getCashIncomeReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getCashIncomeReport(principal.getName(), dates[0], dates[1]));
    }

    /** 9. Kassadan məxaric hesabatı */
    @GetMapping("/cash-expense")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CashExpenseReportDto>> getCashExpenseReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getCashExpenseReport(principal.getName(), dates[0], dates[1]));
    }

    /** 10. Bütün hesabatların ümumi xülasəsi */
    @GetMapping("/full")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportSummaryDto> getFullReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getFullReport(principal.getName(), dates[0], dates[1]));
    }

    /** 11. Tikinti layihələri hesabatı */
    @GetMapping("/construction")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConstructionReportDto>> getConstructionReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getConstructionReport(principal.getName(), dates[0], dates[1]));
    }

    /** 12. Xidmətlər hesabatı */
    @GetMapping("/service")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceReportDto>> getServiceReport(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = resolveDates(startDate, endDate);
        return ResponseEntity.ok(reportService.getServiceReport(principal.getName(), dates[0], dates[1]));
    }

    // ——— Yardımçı: tarix default dəyərləri ———
    private LocalDate[] resolveDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.of(1970, 1, 1); // Bütün dövrlər üçün başlanğıc tarix
        }
        if (endDate == null) {
            endDate = LocalDate.of(2099, 12, 31); // Bütün dövrlər üçün son tarix
        }
        return new LocalDate[]{startDate, endDate};
    }
}
