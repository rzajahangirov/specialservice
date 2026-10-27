package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.dashboard.DashboardSummaryDto;
import az.techvibeds.specialservice.dtos.dashboard.RecentActivityDto;
import az.techvibeds.specialservice.dtos.dashboard.SalesTrendDto;
import az.techvibeds.specialservice.dtos.dashboard.WarehouseLevelDto;
import az.techvibeds.specialservice.models.AuditLog;
import az.techvibeds.specialservice.models.Product;
import az.techvibeds.specialservice.models.Subscription;
import az.techvibeds.specialservice.models.User;
import az.techvibeds.specialservice.repositories.AuditLogRepository;
import az.techvibeds.specialservice.repositories.FinancialTransactionRepository;
import az.techvibeds.specialservice.repositories.InstallmentRepository;
import az.techvibeds.specialservice.repositories.IndirectExpenseRepository;
import az.techvibeds.specialservice.repositories.PayrollRepository;
import az.techvibeds.specialservice.repositories.ProductRepository;
import az.techvibeds.specialservice.repositories.ProjectExpenseRepository;
import az.techvibeds.specialservice.repositories.SubscriptionRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.repositories.InvoiceItemRepository;
import az.techvibeds.specialservice.services.DashboardService;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.models.FinancialTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final AuditLogRepository auditLogRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PayrollRepository payrollRepository;
    private final ProjectExpenseRepository projectExpenseRepository;
    private final IndirectExpenseRepository indirectExpenseRepository;
    private final InstallmentRepository installmentRepository;
    private final UserRepository userRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final InvoiceItemRepository invoiceItemRepository;

    private static final String[] AZ_MONTHS = {"Yan", "Fev", "Mar", "Apr", "May", "Iyn", "Iyl", "Avq", "Sen", "Okt", "Noy", "Dek"};

    @Override
    public DashboardSummaryDto getSummary(String userEmail, String filterType, LocalDate startDate, LocalDate endDate) {
        User user = findUser(userEmail);
        Long companyId = user.getCompany().getId();
        DateRange currentRange = resolveDateRange(filterType, startDate, endDate);

        BigDecimal totalRevenue = normalize(financialTransactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, currentRange.startDate().atStartOfDay(), currentRange.endDate().atTime(23, 59, 59)));

        BigDecimal totalProjectExpense = normalize(projectExpenseRepository.sumByCompanyAndDateRange(
                companyId, currentRange.startDate(), currentRange.endDate()));

        BigDecimal totalIndirectExpense = normalize(indirectExpenseRepository.sumByCompanyAndDateRange(
                companyId,
                toUtilDate(currentRange.startDate().atStartOfDay()),
                toUtilDate(currentRange.endDate().atTime(23, 59, 59))));

        BigDecimal totalPayroll = sumPayrollForRange(companyId, currentRange.startDate(), currentRange.endDate());
        BigDecimal totalExpense = totalPayroll.add(totalProjectExpense).add(totalIndirectExpense);
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        DateRange previousRange = currentRange.previousRange();
        BigDecimal previousRevenue = normalize(financialTransactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, previousRange.startDate().atStartOfDay(), previousRange.endDate().atTime(23, 59, 59)));
        BigDecimal previousProjectExpense = normalize(projectExpenseRepository.sumByCompanyAndDateRange(
                companyId, previousRange.startDate(), previousRange.endDate()));
        BigDecimal previousIndirectExpense = normalize(indirectExpenseRepository.sumByCompanyAndDateRange(
                companyId,
                toUtilDate(previousRange.startDate().atStartOfDay()),
                toUtilDate(previousRange.endDate().atTime(23, 59, 59))));
        BigDecimal previousPayroll = sumPayrollForRange(companyId, previousRange.startDate(), previousRange.endDate());
        BigDecimal previousExpense = previousPayroll.add(previousProjectExpense).add(previousIndirectExpense);
        BigDecimal previousProfit = previousRevenue.subtract(previousExpense);

        BigDecimal expectedPayments = normalize(installmentRepository.sumUpcomingUnpaidByCompanyAndDueDate(
                companyId, LocalDate.now(), LocalDate.now().plusDays(30)));

        return DashboardSummaryDto.builder()
                .totalRevenue(totalRevenue)
                .revenueTrend(buildTrendText(totalRevenue, previousRevenue, currentRange.label()))
                .totalExpense(totalExpense)
                .expenseTrend(buildTrendText(totalExpense, previousExpense, currentRange.label()))
                .netProfit(netProfit)
                .profitTrend(buildTrendText(netProfit, previousProfit, currentRange.label()))
                .expectedPayments(expectedPayments)
                .expectedPaymentsInfo("Yaxin 30 gun erzinde")
                .build();
    }

    @Override
    public List<SalesTrendDto> getSalesTrend(String userEmail, String filterType, LocalDate startDate, LocalDate endDate) {
        User user = findUser(userEmail);
        Long companyId = user.getCompany().getId();
        DateRange selectedRange = resolveDateRange(filterType, startDate, endDate);

        YearMonth endMonth = YearMonth.from(selectedRange.endDate());
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            months.add(endMonth.minusMonths(i));
        }

        YearMonth firstMonth = months.get(0);
        LocalDate firstDate = firstMonth.atDay(1);
        LocalDate lastDate = endMonth.atEndOfMonth();

        List<FinancialTransaction> transactions = financialTransactionRepository.findByCompanyId(companyId);
        Map<YearMonth, BigDecimal> monthlyTotals = new HashMap<>();

        for (FinancialTransaction transaction : transactions) {
            if (transaction.getType() != TransactionType.INCOME || transaction.getTransactionDate() == null || transaction.getAmount() == null) {
                continue;
            }
            LocalDate date = transaction.getTransactionDate().toLocalDate();
            if (date.isBefore(firstDate) || date.isAfter(lastDate)) {
                continue;
            }
            YearMonth month = YearMonth.from(date);
            monthlyTotals.put(month, monthlyTotals.getOrDefault(month, BigDecimal.ZERO).add(transaction.getAmount()));
        }

        List<SalesTrendDto> result = new ArrayList<>();
        for (YearMonth month : months) {
            result.add(new SalesTrendDto(
                    AZ_MONTHS[month.getMonthValue() - 1],
                    monthlyTotals.getOrDefault(month, BigDecimal.ZERO)));
        }
        return result;
    }

    @Override
    public List<WarehouseLevelDto> getWarehouseLevels(String userEmail) {
        User user = findUser(userEmail);
        List<Product> allProducts = productRepository.findAllByCompany_Id(user.getCompany().getId());
        Map<String, Integer> categoryStockMap = new HashMap<>();

        for (Product product : allProducts) {
            if (product.getCategory() != null && product.getStock() != null) {
                String catName = product.getCategory().getName();
                categoryStockMap.put(catName,
                        categoryStockMap.getOrDefault(catName, 0) + product.getStock().intValue());
            }
        }

        return categoryStockMap.entrySet().stream()
                .map(entry -> new WarehouseLevelDto(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getQuantity().compareTo(a.getQuantity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<az.techvibeds.specialservice.dtos.dashboard.TopSellingCategoryDto> getTopSellingCategories(String userEmail) {
        User user = findUser(userEmail);
        Long companyId = user.getCompany().getId();
        return invoiceItemRepository.findTopSellingCategories(companyId);
    }

    @Override
    public List<RecentActivityDto> getRecentActivities(String userEmail) {
        User user = findUser(userEmail);
        Long companyId = user.getCompany().getId();

        List<String> companyEmails = userRepository.findAllByCompanyId(companyId).stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (companyEmails.isEmpty()) {
            companyEmails = List.of(userEmail);
        }

        Page<AuditLog> auditLogsPage = auditLogRepository.findRecentByActorEmails(
                companyEmails,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp")));

        return auditLogsPage.getContent().stream()
                .map(log -> new RecentActivityDto(
                        log.getAction() != null ? log.getAction() : "Emeliyyat",
                        log.getTimestamp() != null ? log.getTimestamp() : LocalDateTime.now(),
                        deriveStatus(log.getAction())))
                .collect(Collectors.toList());
    }

    private BigDecimal sumPayrollForRange(Long companyId, LocalDate startDate, LocalDate endDate) {
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        return payrollRepository.findAllByFilters(companyId, null, null).stream()
                .filter(p -> p.getPeriod() != null)
                .filter(p -> !p.getPeriod().isBefore(startMonth) && !p.getPeriod().isAfter(endMonth))
                .map(p -> p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private DateRange resolveDateRange(String filterType, LocalDate startDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();
        String normalized = filterType == null ? "last_30_days" : filterType.trim().toLowerCase(Locale.ROOT);

        DateRange range;
        switch (normalized) {
            case "today":
            case "bu_gun":
            case "bugun":
                range = new DateRange(now, now, "Bu gun");
                break;
            case "last_7_days":
            case "son_7_gun":
            case "7d":
                range = new DateRange(now.minusDays(6), now, "Son 7 gun");
                break;
            case "custom":
            case "xususi":
                LocalDate customStart = startDate != null ? startDate : now.minusDays(29);
                LocalDate customEnd = endDate != null ? endDate : now;
                if (customStart.isAfter(customEnd)) {
                    LocalDate temp = customStart;
                    customStart = customEnd;
                    customEnd = temp;
                }
                range = new DateRange(customStart, customEnd, "Xususi dovr");
                break;
            case "this_month":
                range = new DateRange(now.withDayOfMonth(1), now, "Bu ay");
                break;
            case "last_30_days":
            case "son_30_gun":
            case "30d":
            default:
                range = new DateRange(now.minusDays(29), now, "Son 30 gun");
                break;
        }
        return range;
    }

    private String buildTrendText(BigDecimal current, BigDecimal previous, String label) {
        BigDecimal percentChange;
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            percentChange = current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.0") : BigDecimal.ZERO;
        } else {
            percentChange = current.subtract(previous)
                    .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        String sign = percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + percentChange + "% evvelki dovre gore (" + label + ")";
    }

    private String deriveStatus(String action) {
        if (action == null) {
            return "Tamamlandi";
        }
        String lower = action.toLowerCase(Locale.ROOT);
        if (lower.contains("cancel") || lower.contains("legv") || lower.contains("delete")) {
            return "Legv Edildi";
        }
        if (lower.contains("pending") || lower.contains("gozleme") || lower.contains("approve")) {
            return "Gozlemede";
        }
        return "Tamamlandi";
    }

    private BigDecimal normalize(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private java.util.Date toUtilDate(LocalDateTime localDateTime) {
        return java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Istifadeci tapilmadi"));
    }

    private record DateRange(LocalDate startDate, LocalDate endDate, String label) {
        DateRange previousRange() {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate prevEnd = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(days - 1);
            return new DateRange(prevStart, prevEnd, "Evvelki dovr");
        }
    }
}
