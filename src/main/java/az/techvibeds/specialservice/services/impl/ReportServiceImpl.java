package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.finance.FinanceSummaryDto;
import az.techvibeds.specialservice.dtos.finance.MonthlySummaryDto;
import az.techvibeds.specialservice.dtos.report.*;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.FinanceService;
import az.techvibeds.specialservice.services.ModuleAccessService;
import az.techvibeds.specialservice.services.ReportService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hesabatlar modulu — bütün hesabat növlərinin iş məntiqi.
 * Hər metod istifadəçinin şirkətinə aid məlumatları toplayır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final FinanceService financeService;
    private final ModuleAccessService moduleAccessService;
    private final FinancialTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final WarehouseActivityRepository warehouseActivityRepository;
    private final IndirectExpenseRepository indirectExpenseRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final ConstructionProjectRepository constructionProjectRepository;
    private final ServiceRepository serviceRepository;

    // Modul backendKey sabitləri
    private static final String MODULE_FINANCE = "FINANCE";
    private static final String MODULE_INVOICE = "INVOICE";
    private static final String MODULE_INVENTORY = "INVENTORY";
    private static final String MODULE_MANUFACTURING = "MANUFACTURING";
    private static final String MODULE_CONSTRUCTION = "CONSTRUCTION";
    private static final String MODULE_SERVICE = "SERVICE";

    // ═══════════════════════════════════════════════════════════════
    //  KÖHNƏ METODLAR (geriyə uyğunluq üçün saxlanılır)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public FinanceSummaryDto getBalanceSheet(String userEmail) {
        return financeService.getBalanceSheet(userEmail);
    }

    @Override
    public FinanceSummaryDto getProfitLoss(String userEmail) {
        return financeService.getProfitLoss(userEmail);
    }

    @Override
    public FinanceSummaryDto getCashFlow(String userEmail) {
        return financeService.getCashFlow(userEmail);
    }

    @Override
    public byte[] exportExcel(String userEmail) {
        FinanceSummaryDto summary = financeService.getFinanceSummary(userEmail);
        User user = findUserByEmail(userEmail);
        List<FinancialTransaction> transactions = transactionRepository
                .findByCompanyIdOrderByTransactionDateDesc(user.getCompany().getId());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("Summary");
            writeSummary(summarySheet, summary);

            Sheet chartSheet = workbook.createSheet("MonthlyChart");
            writeMonthlyChart(chartSheet, summary.getMonthlySummaries());

            Sheet txSheet = workbook.createSheet("Transactions");
            writeTransactions(txSheet, transactions);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel yaradılarkən xəta baş verdi", e);
        }
    }

    @Override
    public byte[] exportPdf(String userEmail) {
        FinanceSummaryDto summary = financeService.getFinanceSummary(userEmail);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();
            document.add(new Paragraph("Finance Report"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Income: " + summary.getTotalIncome()));
            document.add(new Paragraph("Total Expense: " + summary.getTotalExpense()));
            document.add(new Paragraph("Current Balance: " + summary.getCurrentBalance()));
            document.add(new Paragraph("Net Profit: " + summary.getNetProfit()));
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("PDF yaradılarkən xəta baş verdi", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  1. MALIN HƏRƏKƏTİ HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<ProductMovementDto> getProductMovementReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVENTORY)) {
            log.info("Company {} has no access to INVENTORY module, returning empty product movement report", companyId);
            return Collections.emptyList();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Product> products = productRepository.findAllByCompany_Id(companyId);
        List<ProductMovementDto> result = new ArrayList<>();

        for (Product product : products) {
            Integer incoming = warehouseActivityRepository.sumIncomingByProductAndDateRange(
                    product.getId(), companyId, startDateTime, endDateTime);
            Integer outgoing = warehouseActivityRepository.sumOutgoingByProductAndDateRange(
                    product.getId(), companyId, startDateTime, endDateTime);
            Integer transfer = warehouseActivityRepository.sumTransferByProductAndDateRange(
                    product.getId(), companyId, startDateTime, endDateTime);

            int currentStock = product.getStock() != null ? product.getStock().intValue() : 0;
            int openingStock = currentStock - safeInt(incoming) + safeInt(outgoing);

            // Alış qiyməti: product.purchasePrice (alış qaiməsindən gəlir).
            // Yoxdursa ZERO — satış qiymətinə fallback etmirik (məlumatları qarışdırmamaq üçün).
            BigDecimal purchasePrice = product.getPurchasePrice() != null
                    ? product.getPurchasePrice()
                    : BigDecimal.ZERO;

            BigDecimal totalCostValue = purchasePrice.multiply(BigDecimal.valueOf(currentStock));

            result.add(ProductMovementDto.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productCode(product.getProductCode())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .productType(product.getProductType() != null ? product.getProductType().name() : "GOODS")
                    .openingStock(Math.max(openingStock, 0))
                    .totalIncoming(safeInt(incoming))
                    .totalOutgoing(safeInt(outgoing))
                    .totalTransfer(safeInt(transfer))
                    .closingStock(currentStock)
                    .unitPrice(product.getPrice())
                    .totalValue(product.getPrice() != null
                            ? product.getPrice().multiply(BigDecimal.valueOf(currentStock))
                            : BigDecimal.ZERO)
                    .purchasePrice(purchasePrice)
                    .totalCostValue(totalCostValue)
                    .build());

        }
        log.info("Product movement report generated for company {}: {} products", companyId, result.size());
        return result;
    }

    @Override
    public List<ProductMovementDetailReportDto> getProductMovementDetailsReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVENTORY)) {
            log.info("Company {} has no access to INVENTORY module, returning empty product movement details report", companyId);
            return Collections.emptyList();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<WarehouseActivity> activities = warehouseActivityRepository
                .findAllByCompanyIdAndDateBetweenOrderByDateDesc(companyId, startDateTime, endDateTime);

        return activities.stream().map(activity -> {
            Product product = null;
            if (activity.getProduct() != null) {
                product = activity.getProduct().getProduct();
            }

            BigDecimal purchasePrice = activity.getPurchasePrice();
            BigDecimal salePrice = activity.getSalePrice();

            if (purchasePrice == null && product != null) {
                purchasePrice = product.getPrice();
            }
            if (salePrice == null && product != null) {
                salePrice = product.getPrice();
            }

            int qty = activity.getQuantity() != null ? activity.getQuantity() : 0;
            BigDecimal totalPurchaseValue = purchasePrice != null
                    ? purchasePrice.multiply(BigDecimal.valueOf(qty))
                    : BigDecimal.ZERO;
            BigDecimal totalSaleValue = salePrice != null
                    ? salePrice.multiply(BigDecimal.valueOf(qty))
                    : BigDecimal.ZERO;

            return ProductMovementDetailReportDto.builder()
                    .activityId(activity.getId())
                    .productId(product != null ? product.getId() : null)
                    .productName(product != null ? product.getName() : null)
                    .productCode(product != null ? product.getProductCode() : null)
                    .categoryName(product != null && product.getCategory() != null ? product.getCategory().getName() : null)
                    .activityType(activity.getType() != null ? activity.getType().name() : null)
                    .quantity(qty)
                    .sourceName(activity.getSourceName())
                    .destinationName(activity.getDestinationName())
                    .referenceNumber(activity.getReferenceNumber())
                    .date(activity.getDate())
                    .purchasePrice(purchasePrice)
                    .salePrice(salePrice)
                    .totalPurchaseValue(totalPurchaseValue)
                    .totalSaleValue(totalSaleValue)
                    .build();
        }).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. SATIŞ HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<SalesReportDto> getSalesReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVOICE)) {
            log.info("Company {} has no access to INVOICE module, returning empty sales report", companyId);
            return Collections.emptyList();
        }
        List<Invoice> salesInvoices = invoiceRepository.findSalesInvoicesByCompanyAndDateRange(
                companyId, startDate, endDate);

        List<SalesReportDto> result = salesInvoices.stream().map(invoice ->
                SalesReportDto.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .invoiceDate(invoice.getInvoiceDate())
                        .partnerName(invoice.getPartner() != null ? invoice.getPartner().getName() : "N/A")
                        .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                        .paymentMethod(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : null)
                        .currency(invoice.getCurrency())
                        .totalAmount(invoice.getTotalAmount())
                        .paidAmount(invoice.getPaidAmount())
                        .remainingAmount(invoice.getRemainingAmount())
                        .itemCount(invoice.getItems() != null ? invoice.getItems().size() : 0)
                        .build()
        ).collect(Collectors.toList());

        log.info("Sales report generated for company {}: {} invoices", companyId, result.size());
        return result;
    }

    @Override
    public List<DetailedSalesReportDto> getDetailedSalesReport(
            String userEmail, LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVOICE)) {
            log.info("Company {} has no access to INVOICE module, returning empty detailed sales report", companyId);
            return Collections.emptyList();
        }
        List<Invoice> invoices = invoiceRepository.findInvoicesByFilters(
                companyId, InvoiceType.SALES, startDate, endDate, minAmount, maxAmount);

        List<DetailedSalesReportDto> result = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (invoice.getItems() != null) {
                for (InvoiceItem item : invoice.getItems()) {
                    result.add(DetailedSalesReportDto.builder()
                            .invoiceId(invoice.getId())
                            .invoiceNumber(invoice.getInvoiceNumber())
                            .invoiceDate(invoice.getInvoiceDate())
                            .partnerName(invoice.getPartner() != null ? invoice.getPartner().getName() : "N/A")
                            .productName(item.getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .currency(invoice.getCurrency())
                            .build());
                }
            }
        }

        log.info("Detailed sales report generated for company {}: {} items", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. ALIŞ HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<PurchaseReportDto> getPurchaseReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVOICE)) {
            log.info("Company {} has no access to INVOICE module, returning empty purchase report", companyId);
            return Collections.emptyList();
        }
        List<Invoice> purchaseInvoices = invoiceRepository.findPurchaseInvoicesByCompanyAndDateRange(
                companyId, startDate, endDate);

        List<PurchaseReportDto> result = purchaseInvoices.stream().map(invoice ->
                PurchaseReportDto.builder()
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .invoiceDate(invoice.getInvoiceDate())
                        .partnerName(invoice.getPartner() != null ? invoice.getPartner().getName() : "N/A")
                        .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                        .paymentMethod(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : null)
                        .currency(invoice.getCurrency())
                        .totalAmount(invoice.getTotalAmount())
                        .paidAmount(invoice.getPaidAmount())
                        .remainingAmount(invoice.getRemainingAmount())
                        .itemCount(invoice.getItems() != null ? invoice.getItems().size() : 0)
                        .build()
        ).collect(Collectors.toList());

        log.info("Purchase report generated for company {}: {} invoices", companyId, result.size());
        return result;
    }

    @Override
    public List<DetailedPurchaseReportDto> getDetailedPurchaseReport(
            String userEmail, LocalDate startDate, LocalDate endDate, BigDecimal minAmount, BigDecimal maxAmount) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_INVOICE)) {
            log.info("Company {} has no access to INVOICE module, returning empty detailed purchase report", companyId);
            return Collections.emptyList();
        }
        List<Invoice> invoices = invoiceRepository.findInvoicesByFilters(
                companyId, InvoiceType.PURCHASE, startDate, endDate, minAmount, maxAmount);

        List<DetailedPurchaseReportDto> result = new ArrayList<>();
        for (Invoice invoice : invoices) {
            if (invoice.getItems() != null) {
                for (InvoiceItem item : invoice.getItems()) {
                    result.add(DetailedPurchaseReportDto.builder()
                            .invoiceId(invoice.getId())
                            .invoiceNumber(invoice.getInvoiceNumber())
                            .invoiceDate(invoice.getInvoiceDate())
                            .partnerName(invoice.getPartner() != null ? invoice.getPartner().getName() : "N/A")
                            .productName(item.getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .primeCost(item.getPrimeCost())
                            .currency(invoice.getCurrency())
                            .build());
                }
            }
        }

        log.info("Detailed purchase report generated for company {}: {} items", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  4. DÖVRİYYƏ HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public TurnoverReportDto getTurnoverReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_FINANCE)) {
            log.info("Company {} has no access to FINANCE module, returning empty turnover report", companyId);
            return TurnoverReportDto.builder()
                    .totalSales(BigDecimal.ZERO).totalPurchases(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO).totalCashIncome(BigDecimal.ZERO)
                    .totalCashExpense(BigDecimal.ZERO).grossProfit(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO).turnoverBalance(BigDecimal.ZERO)
                    .build();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Date startDateLegacy = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateLegacy = Date.from(endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        BigDecimal totalSales = invoiceRepository.sumSalesTotalByCompanyAndDateRange(companyId, startDate, endDate);
        BigDecimal totalPurchases = invoiceRepository.sumPurchaseTotalByCompanyAndDateRange(companyId, startDate, endDate);
        BigDecimal totalExpenses = indirectExpenseRepository.sumByCompanyAndDateRange(companyId, startDateLegacy, endDateLegacy);
        BigDecimal totalCashIncome = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, startDateTime, endDateTime);
        BigDecimal totalCashExpense = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.EXPENSE, startDateTime, endDateTime);

        BigDecimal grossProfit = safeBigDecimal(totalSales).subtract(safeBigDecimal(totalPurchases));
        BigDecimal netProfit = grossProfit.subtract(safeBigDecimal(totalExpenses));
        BigDecimal turnoverBalance = safeBigDecimal(totalCashIncome).subtract(safeBigDecimal(totalCashExpense));

        log.info("Turnover report generated for company {}", companyId);
        return TurnoverReportDto.builder()
                .totalSales(safeBigDecimal(totalSales))
                .totalPurchases(safeBigDecimal(totalPurchases))
                .totalExpenses(safeBigDecimal(totalExpenses))
                .totalCashIncome(safeBigDecimal(totalCashIncome))
                .totalCashExpense(safeBigDecimal(totalCashExpense))
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .turnoverBalance(turnoverBalance)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  5. XƏRC HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<ExpenseReportDto> getExpenseReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_FINANCE)) {
            log.info("Company {} has no access to FINANCE module, returning empty expense report", companyId);
            return Collections.emptyList();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Date startDateLegacy = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateLegacy = Date.from(endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        // 1. Dolayı xərcləri gətir (Indirect Expenses)
        List<IndirectExpense> indirectExpenses = indirectExpenseRepository.findByCompanyIdAndDateRange(
                companyId, startDateLegacy, endDateLegacy);

        List<ExpenseReportDto> result = indirectExpenses.stream().map(expense ->
                ExpenseReportDto.builder()
                        .id(expense.getId())
                        .name(expense.getName())
                        .category(expense.getCategory() != null ? expense.getCategory().name() : "Dolayı Xərc")
                        .amount(expense.getAmount())
                        .currency(expense.getCurrency() != null ? expense.getCurrency().name() : "AZN")
                        .expenseDate(expense.getExpenseDate())
                        .build()
        ).collect(Collectors.toCollection(ArrayList::new));

        // 2. Kassa məxaric əməliyyatlarını gətir (Financial Transaction Expenses)
        List<FinancialTransaction> transactions = transactionRepository
                .findExpenseTransactionsByCompanyAndDateRange(companyId, startDateTime, endDateTime);

        transactions.forEach(tx -> {
            result.add(ExpenseReportDto.builder()
                    .id(tx.getId())
                    .name(tx.getDescription() != null ? tx.getDescription() : "Kassa Məxarici")
                    .category(tx.getCategory() != null ? tx.getCategory().getName() : "Kassa Məxarici")
                    .amount(tx.getAmount())
                    .currency("AZN")
                    .expenseDate(Date.from(tx.getTransactionDate().atZone(ZoneId.systemDefault()).toInstant()))
                    .build());
        });

        // 3. Alış Qaimələrini gətir (Purchase Invoices as Expenses)
        List<Invoice> purchaseInvoices = invoiceRepository.findPurchaseInvoicesByCompanyAndDateRange(
                companyId, startDate, endDate);

        purchaseInvoices.forEach(invoice -> {
            String partnerName = invoice.getPartner() != null ? invoice.getPartner().getName() : "N/A";
            result.add(ExpenseReportDto.builder()
                    .id(invoice.getId())
                    .name("Qaimə №: " + invoice.getInvoiceNumber() + " - " + partnerName)
                    .category("Alış Qaiməsi")
                    .amount(invoice.getTotalAmount())
                    .currency(invoice.getCurrency() != null ? invoice.getCurrency() : "AZN")
                    .expenseDate(Date.from(invoice.getInvoiceDate().atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .build());
        });

        // Tarixə görə sırala (azalan sıra)
        result.sort((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()));

        log.info("Merged expense report generated for company {}: {} items", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  6. İSTEHSAL OLUNAN MAL (MƏHSUL) HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<ManufacturedProductReportDto> getManufacturedProductReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_MANUFACTURING)) {
            log.info("Company {} has no access to MANUFACTURING module, returning empty manufactured product report", companyId);
            return Collections.emptyList();
        }
        Date startDateLegacy = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateLegacy = Date.from(endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        List<ProductionOrder> orders = productionOrderRepository.findByCompanyIdAndDateRange(
                companyId, startDateLegacy, endDateLegacy);

        List<ManufacturedProductReportDto> result = new java.util.ArrayList<>();
        for (ProductionOrder order : orders) {
            if (order.getItems() != null) {
                for (ProductionOrderItem item : order.getItems()) {
                    result.add(ManufacturedProductReportDto.builder()
                            .orderId(order.getId())
                            .productName(item.getManufacturedProduct() != null
                                    ? item.getManufacturedProduct().getName() : "N/A")
                            .productDescription(item.getManufacturedProduct() != null
                                    ? item.getManufacturedProduct().getDescription() : null)
                            .quantity(item.getQuantity())
                            .status(order.getStatus() != null ? order.getStatus().name() : null)
                            .progressPercentage(order.getProgressPercentage())
                            .startDate(order.getStartDate())
                            .finishDate(order.getFinishDate())
                            .build());
                }
            }
        }

        log.info("Manufactured product report generated for company {}: {} items", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  7. MƏNFƏƏT / ZƏRƏR HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public ProfitLossReportDto getProfitLossReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_FINANCE)) {
            log.info("Company {} has no access to FINANCE module, returning empty profit/loss report", companyId);
            return ProfitLossReportDto.builder()
                    .totalSalesRevenue(BigDecimal.ZERO).totalPurchaseCost(BigDecimal.ZERO)
                    .grossProfit(BigDecimal.ZERO).totalIndirectExpenses(BigDecimal.ZERO)
                    .totalCashIncome(BigDecimal.ZERO).totalCashExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO).isProfit(true)
                    .build();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Date startDateLegacy = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDateLegacy = Date.from(endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());

        BigDecimal totalSales = safeBigDecimal(invoiceRepository.sumSalesTotalByCompanyAndDateRange(companyId, startDate, endDate));
        BigDecimal totalPurchases = safeBigDecimal(invoiceRepository.sumPurchaseTotalByCompanyAndDateRange(companyId, startDate, endDate));
        BigDecimal totalIndirectExpenses = safeBigDecimal(indirectExpenseRepository.sumByCompanyAndDateRange(companyId, startDateLegacy, endDateLegacy));
        BigDecimal totalCashIncome = safeBigDecimal(transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, startDateTime, endDateTime));
        BigDecimal totalCashExpense = safeBigDecimal(transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.EXPENSE, startDateTime, endDateTime));

        BigDecimal grossProfit = totalSales.subtract(totalPurchases);
        BigDecimal netProfit = grossProfit.subtract(totalIndirectExpenses);

        log.info("Profit/Loss report generated for company {}: net={}", companyId, netProfit);
        return ProfitLossReportDto.builder()
                .totalSalesRevenue(totalSales)
                .totalPurchaseCost(totalPurchases)
                .grossProfit(grossProfit)
                .totalIndirectExpenses(totalIndirectExpenses)
                .totalCashIncome(totalCashIncome)
                .totalCashExpense(totalCashExpense)
                .netProfit(netProfit)
                .isProfit(netProfit.compareTo(BigDecimal.ZERO) >= 0)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  8. KASSAYA MƏDAXİL HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<CashIncomeReportDto> getCashIncomeReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_FINANCE)) {
            log.info("Company {} has no access to FINANCE module, returning empty cash income report", companyId);
            return Collections.emptyList();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<FinancialTransaction> transactions = transactionRepository
                .findIncomeTransactionsByCompanyAndDateRange(companyId, startDateTime, endDateTime);

        List<CashIncomeReportDto> result = transactions.stream().map(tx -> {
            String partnerName = null;
            if (tx.getPartner() != null) {
                partnerName = tx.getPartner().getName();
            } else if (tx.getInvoice() != null && tx.getInvoice().getPartner() != null) {
                partnerName = tx.getInvoice().getPartner().getName();
            }
            return CashIncomeReportDto.builder()
                    .transactionId(tx.getId())
                    .amount(tx.getAmount())
                    .transactionDate(tx.getTransactionDate())
                    .description(tx.getDescription())
                    .categoryName(tx.getCategory() != null ? tx.getCategory().getName() : null)
                    .accountName(tx.getAccount() != null ? tx.getAccount().getName() : null)
                    .partnerName(partnerName)
                    .build();
        }).collect(Collectors.toList());

        log.info("Cash income report generated for company {}: {} transactions", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  9. KASSADAN MƏXARİC HESABATI
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<CashExpenseReportDto> getCashExpenseReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_FINANCE)) {
            log.info("Company {} has no access to FINANCE module, returning empty cash expense report", companyId);
            return Collections.emptyList();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<FinancialTransaction> transactions = transactionRepository
                .findExpenseTransactionsByCompanyAndDateRange(companyId, startDateTime, endDateTime);

        List<CashExpenseReportDto> result = transactions.stream().map(tx -> {
            String partnerName = null;
            if (tx.getPartner() != null) {
                partnerName = tx.getPartner().getName();
            } else if (tx.getInvoice() != null && tx.getInvoice().getPartner() != null) {
                partnerName = tx.getInvoice().getPartner().getName();
            }
            return CashExpenseReportDto.builder()
                    .transactionId(tx.getId())
                    .amount(tx.getAmount())
                    .transactionDate(tx.getTransactionDate())
                    .description(tx.getDescription())
                    .categoryName(tx.getCategory() != null ? tx.getCategory().getName() : null)
                    .accountName(tx.getAccount() != null ? tx.getAccount().getName() : null)
                    .partnerName(partnerName)
                    .build();
        }).collect(Collectors.toList());

        log.info("Cash expense report generated for company {}: {} transactions", companyId, result.size());
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  10. BÜTÜN HESABATLARIN ÜMUMİ XÜLASƏSİ
    // ═══════════════════════════════════════════════════════════════

    @Override
    public ReportSummaryDto getFullReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = findUserByEmail(userEmail);
        String companyName = user.getCompany() != null ? user.getCompany().getName() : "N/A";
        Long companyId = user.getCompany() != null ? user.getCompany().getId() : null;

        // Şirkətin aktiv modullarını bir dəfə çəkirik — hər hesabat ayrıca yoxlamır
        Set<String> activeModuleKeys = moduleAccessService.getActiveModuleKeys(companyId);
        log.info("Full report: company {} has active modules: {}", companyId, activeModuleKeys);

        boolean hasFinance = activeModuleKeys.contains(MODULE_FINANCE);
        boolean hasInvoice = activeModuleKeys.contains(MODULE_INVOICE);
        boolean hasInventory = activeModuleKeys.contains(MODULE_INVENTORY);
        boolean hasManufacturing = activeModuleKeys.contains(MODULE_MANUFACTURING);
        boolean hasConstruction = activeModuleKeys.contains(MODULE_CONSTRUCTION);
        boolean hasService = activeModuleKeys.contains(MODULE_SERVICE);

        ProfitLossReportDto profitLoss = hasFinance
                ? getProfitLossReport(userEmail, startDate, endDate)
                : ProfitLossReportDto.builder()
                    .totalSalesRevenue(BigDecimal.ZERO).totalPurchaseCost(BigDecimal.ZERO)
                    .grossProfit(BigDecimal.ZERO).totalIndirectExpenses(BigDecimal.ZERO)
                    .totalCashIncome(BigDecimal.ZERO).totalCashExpense(BigDecimal.ZERO)
                    .netProfit(BigDecimal.ZERO).isProfit(true)
                    .build();

        String period = (startDate.equals(LocalDate.of(1970, 1, 1)) && endDate.equals(LocalDate.of(2099, 12, 31)))
                ? "Bütün dövrlər"
                : startDate.toString() + " — " + endDate.toString();

        return ReportSummaryDto.builder()
                .companyName(companyName)
                .reportPeriod(period)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .totalSales(profitLoss.getTotalSalesRevenue())
                .totalPurchases(profitLoss.getTotalPurchaseCost())
                .totalExpenses(profitLoss.getTotalIndirectExpenses())
                .totalCashIncome(profitLoss.getTotalCashIncome())
                .totalCashExpense(profitLoss.getTotalCashExpense())
                .netProfit(profitLoss.getNetProfit())
                .isProfit(profitLoss.isProfit())
                // Hər bölmə yalnız icazəli modula aiddisə doldurulur
                .productMovements(hasInventory ? getProductMovementReport(userEmail, startDate, endDate) : null)
                .salesReport(hasInvoice ? getSalesReport(userEmail, startDate, endDate) : null)
                .purchaseReport(hasInvoice ? getPurchaseReport(userEmail, startDate, endDate) : null)
                .turnoverReport(hasFinance ? getTurnoverReport(userEmail, startDate, endDate) : null)
                .expenseReport(hasFinance ? getExpenseReport(userEmail, startDate, endDate) : null)
                .manufacturedProducts(hasManufacturing ? getManufacturedProductReport(userEmail, startDate, endDate) : null)
                .profitLossReport(hasFinance ? profitLoss : null)
                .cashIncomeReport(hasFinance ? getCashIncomeReport(userEmail, startDate, endDate) : null)
                .cashExpenseReport(hasFinance ? getCashExpenseReport(userEmail, startDate, endDate) : null)
                .constructionReport(hasConstruction ? getConstructionReport(userEmail, startDate, endDate) : null)
                .serviceReport(hasService ? getServiceReport(userEmail, startDate, endDate) : null)
                // Frontend üçün: hansı modullar aktivdir
                .accessibleModules(activeModuleKeys)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    //  YARDIMÇI METODLAR
    // ═══════════════════════════════════════════════════════════════

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı: " + email));
    }

    private Long getCompanyId(String userEmail) {
        User user = findUserByEmail(userEmail);
        if (user.getCompany() == null) {
            throw new RuntimeException("İstifadəçinin şirkəti yoxdur: " + userEmail);
        }
        return user.getCompany().getId();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // ——— Excel yazma metodları (köhnə) ———

    private void writeSummary(Sheet sheet, FinanceSummaryDto summary) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Metric");
        h.createCell(1).setCellValue("Amount");

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Total Income");
        r1.createCell(1).setCellValue(toDouble(summary.getTotalIncome()));

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Total Expense");
        r2.createCell(1).setCellValue(toDouble(summary.getTotalExpense()));

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Current Balance");
        r3.createCell(1).setCellValue(toDouble(summary.getCurrentBalance()));

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Net Profit");
        r4.createCell(1).setCellValue(toDouble(summary.getNetProfit()));

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeMonthlyChart(Sheet sheet, List<MonthlySummaryDto> monthlySummaries) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("Month");
        h.createCell(1).setCellValue("Income");
        h.createCell(2).setCellValue("Expense");

        if (monthlySummaries == null) {
            return;
        }

        int rowNum = 1;
        for (MonthlySummaryDto m : monthlySummaries) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(m.getMonth());
            row.createCell(1).setCellValue(toDouble(m.getIncome()));
            row.createCell(2).setCellValue(toDouble(m.getExpense()));
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private void writeTransactions(Sheet sheet, List<FinancialTransaction> transactions) {
        Row h = sheet.createRow(0);
        h.createCell(0).setCellValue("ID");
        h.createCell(1).setCellValue("Date");
        h.createCell(2).setCellValue("Type");
        h.createCell(3).setCellValue("Amount");
        h.createCell(4).setCellValue("Category");
        h.createCell(5).setCellValue("Description");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int rowNum = 1;
        for (FinancialTransaction tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tx.getId());
            row.createCell(1).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().format(formatter) : "");
            row.createCell(2).setCellValue(tx.getType() != null ? tx.getType().name() : "");
            row.createCell(3).setCellValue(toDouble(tx.getAmount()));
            row.createCell(4).setCellValue(tx.getCategory() != null ? tx.getCategory().getName() : "");
            row.createCell(5).setCellValue(tx.getDescription() != null ? tx.getDescription() : "");
        }

        for (int i = 0; i <= 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0D : value.doubleValue();
    }

    @Override
    public List<ConstructionReportDto> getConstructionReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        Long companyId = getCompanyId(userEmail);
        if (!moduleAccessService.hasModuleAccess(companyId, MODULE_CONSTRUCTION)) {
            log.info("Company {} has no access to CONSTRUCTION module, returning empty construction report", companyId);
            return Collections.emptyList();
        }
        List<ConstructionProject> projects = constructionProjectRepository.findAllByCompany_Id(companyId);

        return projects.stream()
                .filter(p -> {
                    LocalDate pStart = p.getStartDate();
                    if (pStart == null) return true;
                    return !pStart.isBefore(startDate) && !pStart.isAfter(endDate);
                })
                .map(p -> {
                    BigDecimal budget = safeBigDecimal(p.getBudget());
                    BigDecimal usedBudget = safeBigDecimal(p.getUsedBudget());
                    BigDecimal remainingBudget = budget.subtract(usedBudget);

                    int stageCount = p.getStages() != null ? p.getStages().size() : 0;
                    int completedStageCount = 0;
                    if (p.getStages() != null) {
                        completedStageCount = (int) p.getStages().stream()
                                .filter(s -> s.getStatus() == az.techvibeds.specialservice.enums.StageStatus.COMPLETED)
                                .count();
                    }

                    return ConstructionReportDto.builder()
                            .projectId(p.getId())
                            .projectName(p.getName())
                            .projectManager(p.getProjectManager())
                            .budget(budget)
                            .usedBudget(usedBudget)
                            .remainingBudget(remainingBudget)
                            .status(p.getStatus() != null ? p.getStatus().name() : null)
                            .startDate(p.getStartDate())
                            .endDate(p.getEndDate())
                            .stageCount(stageCount)
                            .completedStageCount(completedStageCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ServiceReportDto> getServiceReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = findUserByEmail(userEmail);
        if (user.getCompany() == null) {
            throw new RuntimeException("İstifadəçinin şirkəti yoxdur: " + userEmail);
        }
        if (!moduleAccessService.hasModuleAccess(user.getCompany().getId(), MODULE_SERVICE)) {
            log.info("Company {} has no access to SERVICE module, returning empty service report", user.getCompany().getId());
            return Collections.emptyList();
        }
        List<az.techvibeds.specialservice.models.Service> services = serviceRepository.findAllByCompany(user.getCompany());

        return services.stream()
                .filter(s -> {
                    LocalDate deadline = s.getDeadline();
                    if (deadline == null) return true;
                    if (startDate != null && deadline.isBefore(startDate)) return false;
                    if (endDate != null && deadline.isAfter(endDate)) return false;
                    return true;
                })
                .map(s -> ServiceReportDto.builder()
                        .serviceId(s.getId())
                        .serviceName(s.getName())
                        .description(s.getDescription())
                        .amount(s.getAmount())
                        .unitName(s.getUnit() != null ? s.getUnit().getName() : null)
                        .partnerName(s.getPartner() != null ? s.getPartner().getName() : null)
                        .status(s.getStatus() != null ? s.getStatus().name() : null)
                        .assigneeName(s.getAssignee() != null ? s.getAssignee().getName() : null)
                        .deadline(s.getDeadline())
                        .build())
                .collect(Collectors.toList());
    }
}
