package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.finance.FinanceSummaryDto;
import az.techvibeds.specialservice.dtos.finance.TaxCalculateRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxCalculateResponseDto;
import az.techvibeds.specialservice.dtos.finance.TaxPaymentRequestDto;
import az.techvibeds.specialservice.dtos.finance.TaxPeriodType;
import az.techvibeds.specialservice.dtos.finance.TaxXmlExportRequestDto;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.AccountRepository;
import az.techvibeds.specialservice.repositories.FinancialTransactionRepository;
import az.techvibeds.specialservice.repositories.TransactionCategoryRepository;
import az.techvibeds.specialservice.repositories.UserRepository;
import az.techvibeds.specialservice.services.FinanceService;
import az.techvibeds.specialservice.services.TaxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private final FinanceService financeService;
    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public TaxCalculateResponseDto calculateTax(TaxCalculateRequestDto request, String userEmail) {
        TaxPeriodRange range = resolveRange(
                request.getPeriodType(),
                request.getReferenceDate(),
                request.getStartDate(),
                request.getEndDate());

        BigDecimal safeTaxRate = request.getTaxRate() == null ? new BigDecimal("18") : request.getTaxRate();
        BigDecimal taxableAmount = request.getTaxableAmount();
        if (taxableAmount == null) {
            FinanceSummaryDto summary = financeService.getFinanceSummaryForDateRange(
                    userEmail, range.startDate(), range.endDate());
            BigDecimal netProfit = summary.getNetProfit() == null ? BigDecimal.ZERO : summary.getNetProfit();
            taxableAmount = netProfit.max(BigDecimal.ZERO);
        }

        BigDecimal taxAmount = taxableAmount.multiply(safeTaxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        TaxCalculateResponseDto response = new TaxCalculateResponseDto();
        response.setTaxableAmount(taxableAmount);
        response.setTaxRate(safeTaxRate);
        response.setTaxAmount(taxAmount);
        response.setTotalAmount(taxableAmount.add(taxAmount));
        response.setPeriodType(range.periodType());
        response.setPeriodStart(range.startDate());
        response.setPeriodEnd(range.endDate());
        response.setGeneratedAt(LocalDateTime.now());
        response.setPaid(false);
        return response;
    }

    @Override
    @Transactional
    public TaxCalculateResponseDto payTax(TaxPaymentRequestDto request, String userEmail) {
        // 1. Vergini hesabla
        TaxPeriodRange range = resolveRange(
                request.getPeriodType(),
                request.getReferenceDate(),
                request.getStartDate(),
                request.getEndDate());

        BigDecimal safeTaxRate = request.getTaxRate() == null ? new BigDecimal("18") : request.getTaxRate();
        BigDecimal taxableAmount = request.getTaxableAmount();
        if (taxableAmount == null) {
            FinanceSummaryDto summary = financeService.getFinanceSummaryForDateRange(
                    userEmail, range.startDate(), range.endDate());
            BigDecimal netProfit = summary.getNetProfit() == null ? BigDecimal.ZERO : summary.getNetProfit();
            taxableAmount = netProfit.max(BigDecimal.ZERO);
        }

        BigDecimal taxAmount = taxableAmount.multiply(safeTaxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // 2. Kassanı tap və yoxla
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
        Company company = user.getCompany();

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Kassa/Bank hesabı tapılmadı: " + request.getAccountId()));

        if (company != null && !account.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu hesab sizin şirkətə aid deyil!");
        }

        // 3. Vergi kateqoriyasını tap və ya yarat
        TransactionCategory taxCategory = categoryRepository.findByCompanyId(company.getId())
                .stream()
                .filter(c -> "Vergi Ödənişi".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    TransactionCategory newCat = new TransactionCategory();
                    newCat.setName("Vergi Ödənişi");
                    newCat.setType(TransactionType.EXPENSE);
                    newCat.setCompany(company);
                    return categoryRepository.save(newCat);
                });

        // 4. Maliyyə tranzaksiyası yarat (EXPENSE)
        String periodDesc = range.periodType() + " (" + range.startDate() + " - " + range.endDate() + ")";
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(taxAmount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setType(TransactionType.EXPENSE);
        transaction.setDescription("Vergi ödənişi: " + periodDesc + " | Faiz: " + safeTaxRate + "%");
        transaction.setAccount(account);
        transaction.setCategory(taxCategory);
        transaction.setCompany(company);
        transaction.setCreatedBy(user);

        // 5. Kassa balansını azalt
        account.setBalance(account.getBalance().subtract(taxAmount));
        accountRepository.save(account);

        FinancialTransaction saved = transactionRepository.save(transaction);

        // 6. Cavab qur
        TaxCalculateResponseDto response = new TaxCalculateResponseDto();
        response.setTaxableAmount(taxableAmount);
        response.setTaxRate(safeTaxRate);
        response.setTaxAmount(taxAmount);
        response.setTotalAmount(taxableAmount.add(taxAmount));
        response.setPeriodType(range.periodType());
        response.setPeriodStart(range.startDate());
        response.setPeriodEnd(range.endDate());
        response.setGeneratedAt(LocalDateTime.now());
        response.setPaid(true);
        response.setAccountId(account.getId());
        response.setAccountName(account.getName());
        response.setTransactionId(saved.getId());
        return response;
    }

    @Override
    public byte[] exportXml(TaxXmlExportRequestDto request, String userEmail) {
        TaxPeriodRange range = resolveRange(
                request.getPeriodType(),
                request.getReferenceDate(),
                request.getStartDate(),
                request.getEndDate());

        BigDecimal safeTaxRate = request.getTaxRate() == null ? new BigDecimal("18") : request.getTaxRate();
        FinanceSummaryDto summary = financeService.getFinanceSummaryForDateRange(
                userEmail, range.startDate(), range.endDate());

        BigDecimal netProfit = summary.getNetProfit() == null ? BigDecimal.ZERO : summary.getNetProfit();
        BigDecimal taxableAmount = netProfit.max(BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount.multiply(safeTaxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<taxReport>\n"
                + "  <periodType>" + range.periodType() + "</periodType>\n"
                + "  <periodStart>" + range.startDate() + "</periodStart>\n"
                + "  <periodEnd>" + range.endDate() + "</periodEnd>\n"
                + "  <currency>AZN</currency>\n"
                + "  <totalIncome>" + summary.getTotalIncome() + "</totalIncome>\n"
                + "  <totalExpense>" + summary.getTotalExpense() + "</totalExpense>\n"
                + "  <netProfit>" + netProfit + "</netProfit>\n"
                + "  <taxableAmount>" + taxableAmount + "</taxableAmount>\n"
                + "  <taxRate>" + safeTaxRate + "</taxRate>\n"
                + "  <taxAmount>" + taxAmount + "</taxAmount>\n"
                + "  <generatedAt>" + LocalDateTime.now() + "</generatedAt>\n"
                + "</taxReport>\n";

        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private TaxPeriodRange resolveRange(TaxPeriodType periodType,
                                        LocalDate referenceDate,
                                        LocalDate customStart,
                                        LocalDate customEnd) {
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        TaxPeriodType safeType = periodType != null ? periodType : TaxPeriodType.MONTHLY;

        return switch (safeType) {
            case MONTHLY -> new TaxPeriodRange(
                    TaxPeriodType.MONTHLY,
                    ref.withDayOfMonth(1),
                    ref.withDayOfMonth(ref.lengthOfMonth()));
            case QUARTERLY -> {
                int quarterStartMonth = ((ref.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate start = LocalDate.of(ref.getYear(), quarterStartMonth, 1);
                LocalDate end = start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
                yield new TaxPeriodRange(TaxPeriodType.QUARTERLY, start, end);
            }
            case YEARLY -> new TaxPeriodRange(
                    TaxPeriodType.YEARLY,
                    LocalDate.of(ref.getYear(), 1, 1),
                    LocalDate.of(ref.getYear(), 12, 31));
            case CUSTOM -> {
                LocalDate start = customStart != null ? customStart : ref.withDayOfMonth(1);
                LocalDate end = customEnd != null ? customEnd : ref;
                if (start.isAfter(end)) {
                    LocalDate tmp = start;
                    start = end;
                    end = tmp;
                }
                yield new TaxPeriodRange(TaxPeriodType.CUSTOM, start, end);
            }
        };
    }

    private record TaxPeriodRange(TaxPeriodType periodType, LocalDate startDate, LocalDate endDate) {
    }
}
