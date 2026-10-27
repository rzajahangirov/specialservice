package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.finance.*;
import az.techvibeds.specialservice.enums.RecurrenceFrequency;
import az.techvibeds.specialservice.enums.RecurringTemplateStatus;
import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.*;
import az.techvibeds.specialservice.services.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final FinancialTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final RecurringExpenseTemplateRepository recurringExpenseTemplateRepository;

    private static final String[] MONTH_NAMES = {
            "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
            "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"
    };

    // ═══════════════════════════════════════════
    //  DASHBOARD — Şəkildəki 4 kart + qrafik
    // ═══════════════════════════════════════════

    @Override
    public FinanceSummaryDto getFinanceSummary(String userEmail) {
        User user = findUser(userEmail);
        if (user.getCompany() == null) {
            FinanceSummaryDto emptyDto = new FinanceSummaryDto();
            emptyDto.setTotalIncome(BigDecimal.ZERO);
            emptyDto.setTotalExpense(BigDecimal.ZERO);
            emptyDto.setNetProfit(BigDecimal.ZERO);
            emptyDto.setCurrentBalance(BigDecimal.ZERO);
            emptyDto.setMonthlySummaries(new ArrayList<>());
            return emptyDto;
        }
        Long companyId = user.getCompany().getId();

        LocalDateTime now = LocalDateTime.now();
        // Cari ayın əvvəli və sonu
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime currentMonthEnd = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);

        // Əvvəlki ayın əvvəli və sonu
        LocalDateTime prevMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime prevMonthEnd = currentMonthStart.minusSeconds(1);

        FinanceSummaryDto dto = buildSummaryForRange(companyId, currentMonthStart, currentMonthEnd, prevMonthStart, prevMonthEnd);
        BigDecimal currentBalance = calculateCurrentBalance(companyId);
        dto.setCurrentBalance(currentBalance);
        dto.setBalanceChangePercent(BigDecimal.ZERO); // Balans üçün tarixçə yoxdur, sadəcə cari

        // ——— Son 6 aylıq qrafik data ———
        dto.setMonthlySummaries(getLast6MonthsSummary(companyId));

        return dto;
    }

    @Override
    public FinanceSummaryDto getFinanceSummaryForDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        User user = findUser(userEmail);
        if (user.getCompany() == null) return new FinanceSummaryDto();
        Long companyId = user.getCompany().getId();

        LocalDate safeStart = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate safeEnd = endDate != null ? endDate : LocalDate.now();
        if (safeStart.isAfter(safeEnd)) {
            LocalDate tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        LocalDateTime currentStart = safeStart.atStartOfDay();
        LocalDateTime currentEnd = safeEnd.atTime(23, 59, 59);
        long dayCount = java.time.temporal.ChronoUnit.DAYS.between(safeStart, safeEnd) + 1;
        LocalDate previousEndDate = safeStart.minusDays(1);
        LocalDate previousStartDate = previousEndDate.minusDays(dayCount - 1);
        LocalDateTime previousStart = previousStartDate.atStartOfDay();
        LocalDateTime previousEnd = previousEndDate.atTime(23, 59, 59);

        FinanceSummaryDto dto = buildSummaryForRange(companyId, currentStart, currentEnd, previousStart, previousEnd);
        dto.setCurrentBalance(calculateCurrentBalance(companyId));
        dto.setBalanceChangePercent(BigDecimal.ZERO);
        dto.setMonthlySummaries(getLast6MonthsSummary(companyId));
        return dto;
    }

    // ═══════════════════════════════════════════
    //  TRANZAKSİYA CRUD
    // ═══════════════════════════════════════════

    @Override
    @Transactional
    public TransactionReadDto createTransaction(TransactionCreateDto dto, String userEmail) {
        User user = findUser(userEmail);

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Hesab tapılmadı: " + dto.getAccountId()));

        TransactionCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kateqoriya tapılmadı: " + dto.getCategoryId()));

        TransactionType type = TransactionType.valueOf(dto.getType().toUpperCase());

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDateTime.now());
        transaction.setType(type);
        transaction.setDescription(dto.getDescription());
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(user.getCompany());

        // ——— Hesab balansını yenilə ———
        if (type == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(dto.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
        }
        accountRepository.save(account);

        return convertToReadDto(transactionRepository.save(transaction));
    }

    @Override
    public Page<TransactionReadDto> getTransactions(Pageable pageable, String userEmail) {
        User user = findUser(userEmail);
        if (user.getCompany() == null) return Page.empty();
        return transactionRepository.findByCompanyIdOrderByTransactionDateDesc(
                user.getCompany().getId(), pageable).map(this::convertToReadDto);
    }

    @Override
    public TransactionReadDto getTransactionById(Long id, String userEmail) {
        User user = findUser(userEmail);
        FinancialTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tranzaksiya tapılmadı"));
        if (user.getCompany() == null || !transaction.getCompany().getId().equals(user.getCompany().getId())) {
            // If super admin, allow viewing all, otherwise restricted
            if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_GLOBAL_ADMIN"))) {
                 return convertToReadDto(transaction);
            }
            throw new RuntimeException("Bu tranzaksiyaya icazəniz yoxdur");
        }
        return convertToReadDto(transaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id, String userEmail) {
        User user = findUser(userEmail);
        FinancialTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tranzaksiya tapılmadı"));
        if (!transaction.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu tranzaksiyaya icazəniz yoxdur");
        }

        // ——— Balansı geri qaytar ———
        Account account = transaction.getAccount();
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }

    // ═══════════════════════════════════════════
    //  HESAB (Account) CRUD
    // ═══════════════════════════════════════════

    @Override
    @Transactional
    public AccountReadDto createAccount(AccountCreateDto dto, String userEmail) {
        User user = findUser(userEmail);

        Account account = new Account();
        account.setName(dto.getName());
        account.setAccountNumber(dto.getAccountNumber());
        account.setBalance(dto.getInitialBalance() != null ? dto.getInitialBalance() : BigDecimal.ZERO);
        account.setCompany(user.getCompany());

        return convertAccountToDto(accountRepository.save(account));
    }

    @Override
    public List<AccountReadDto> getAccounts(String userEmail) {
        User user = findUser(userEmail);
        if (user.getCompany() == null) return new ArrayList<>();
        return accountRepository.findByCompanyId(user.getCompany().getId())
                .stream().map(this::convertAccountToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountReadDto updateAccount(Long id, AccountUpdateDto dto, String userEmail) {
        User user = findUser(userEmail);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Hesab tapılmadı"));

        if (!account.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu hesaba düzəliş etmək icazəniz yoxdur");
        }

        if (dto.getName() != null) account.setName(dto.getName());
        if (dto.getAccountNumber() != null) account.setAccountNumber(dto.getAccountNumber());
        if (dto.getInitialBalance() != null) account.setBalance(dto.getInitialBalance());

        return convertAccountToDto(accountRepository.save(account));
    }

    @Override
    @Transactional
    public void deleteAccount(Long id, String userEmail) {
        User user = findUser(userEmail);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new az.techvibeds.specialservice.exceptions.ResourceNotFoundException("Hesab tapılmadı"));

        if (!account.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu hesabı silmək icazəniz yoxdur");
        }

        try {
            accountRepository.delete(account);
            accountRepository.flush(); // Ensure integrity constraint is caught here if any
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Bu kassada maliyyə əməliyyatları olduğu üçün silinə bilməz. Zəhmət olmasa, əvvəlcə əməliyyatları silin.");
        }
    }

    // ═══════════════════════════════════════════
    //  KATEQORİYA CRUD
    // ═══════════════════════════════════════════

    @Override
    @Transactional
    public CategoryReadDto createCategory(CategoryCreateDto dto, String userEmail) {
        User user = findUser(userEmail);

        TransactionCategory category = new TransactionCategory();
        category.setName(dto.getName());
        category.setType(TransactionType.valueOf(dto.getType().toUpperCase()));
        category.setCompany(user.getCompany());

        return convertCategoryToDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryReadDto> getCategories(String userEmail) {
        User user = findUser(userEmail);
        if (user.getCompany() == null) return new ArrayList<>();
        return categoryRepository.findByCompanyId(user.getCompany().getId())
                .stream().map(this::convertCategoryToDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════
    //  HESABATLAR (Reports)
    // ═══════════════════════════════════════════

    @Override
    public FinanceSummaryDto getBalanceSheet(String userEmail) {
        User user = findUser(userEmail);
        Long companyId = user.getCompany().getId();
        List<FinancialTransaction> allTransactions = transactionRepository.findByCompanyId(companyId);

        BigDecimal totalIncome = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = allTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FinanceSummaryDto dto = new FinanceSummaryDto();
        dto.setTotalIncome(totalIncome);
        dto.setTotalExpense(totalExpense);
        dto.setCurrentBalance(calculateCurrentBalance(companyId));
        dto.setNetProfit(totalIncome.subtract(totalExpense));
        dto.setIncomeChangePercent(BigDecimal.ZERO);
        dto.setExpenseChangePercent(BigDecimal.ZERO);
        dto.setProfitChangePercent(BigDecimal.ZERO);
        dto.setBalanceChangePercent(BigDecimal.ZERO);
        dto.setMonthlySummaries(getLast6MonthsSummary(companyId));
        return dto;
    }

    @Override
    public FinanceSummaryDto getProfitLoss(String userEmail) {
        return getFinanceSummary(userEmail);
    }

    @Override
    public FinanceSummaryDto getCashFlow(String userEmail) {
        return getFinanceSummaryForDateRange(userEmail, LocalDate.now().minusDays(29), LocalDate.now());
    }

    // ═══════════════════════════════════════════
    //  YARDIMÇI METODLAR (Private Helpers)
    // ═══════════════════════════════════════════

    private BigDecimal calculateCurrentBalance(Long companyId) {
        List<Account> accounts = accountRepository.findByCompanyId(companyId);
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FinanceSummaryDto buildSummaryForRange(Long companyId,
                                                   LocalDateTime currentStart,
                                                   LocalDateTime currentEnd,
                                                   LocalDateTime previousStart,
                                                   LocalDateTime previousEnd) {
        BigDecimal totalIncome = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, currentStart, currentEnd);
        BigDecimal totalExpense = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.EXPENSE, currentStart, currentEnd);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        BigDecimal prevIncome = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.INCOME, previousStart, previousEnd);
        BigDecimal prevExpense = transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                companyId, TransactionType.EXPENSE, previousStart, previousEnd);
        BigDecimal prevProfit = prevIncome.subtract(prevExpense);

        FinanceSummaryDto dto = new FinanceSummaryDto();
        dto.setTotalIncome(totalIncome);
        dto.setTotalExpense(totalExpense);
        dto.setNetProfit(netProfit);
        dto.setIncomeChangePercent(calculateChangePercent(totalIncome, prevIncome));
        dto.setExpenseChangePercent(calculateChangePercent(totalExpense, prevExpense));
        dto.setProfitChangePercent(calculateChangePercent(netProfit, prevProfit));
        return dto;
    }

    /**
     * Əvvəlki dövrə nisbətən dəyişiklik faizini hesablayır.
     * Düstur: ((cari - əvvəlki) / əvvəlki) * 100
     */
    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.0") : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Son 6 ayın aylıq gəlir/xərc xülasəsini qaytarır — şəkildəki qrafik üçün
     */
    private List<MonthlySummaryDto> getLast6MonthsSummary(Long companyId) {
        List<MonthlySummaryDto> summaries = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

            MonthlySummaryDto dto = new MonthlySummaryDto();
            dto.setMonth(MONTH_NAMES[month.getMonthValue() - 1]);
            dto.setIncome(transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                    companyId, TransactionType.INCOME, start, end));
            dto.setExpense(transactionRepository.sumAmountByCompanyAndTypeAndDateBetween(
                    companyId, TransactionType.EXPENSE, start, end));
            summaries.add(dto);
        }
        return summaries;
    }

    // ═══════════════════════════════════════════
    //  TƏKRARLANAN XƏRC ŞABLONLARI
    // ═══════════════════════════════════════════

    @Override
    @Transactional
    public RecurringExpenseReadDto createRecurringExpense(RecurringExpenseCreateDto dto, String userEmail) {
        User user = findUser(userEmail);
        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Hesab tapılmadı"));
        TransactionCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kateqoriya tapılmadı"));

        RecurringExpenseTemplate template = new RecurringExpenseTemplate();
        template.setName(dto.getName());
        template.setAmount(dto.getAmount());
        template.setDescription(dto.getDescription());
        template.setFrequency(RecurrenceFrequency.valueOf(dto.getFrequency().toUpperCase()));
        template.setStartDate(dto.getStartDate());
        template.setEndDate(dto.getEndDate());
        template.setNextExecutionDate(dto.getStartDate());
        template.setAccount(account);
        template.setCategory(category);
        template.setCompany(user.getCompany());
        template.setCreatedBy(user);
        template.setStatus(RecurringTemplateStatus.ACTIVE);

        return convertToRecurringDto(recurringExpenseTemplateRepository.save(template));
    }

    @Override
    @Transactional
    public RecurringExpenseReadDto updateRecurringExpense(Long id, RecurringExpenseUpdateDto dto, String userEmail) {
        User user = findUser(userEmail);
        RecurringExpenseTemplate template = recurringExpenseTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şablon tapılmadı"));

        if (!template.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu şablona icazəniz yoxdur");
        }

        if (dto.getName() != null) template.setName(dto.getName());
        if (dto.getAmount() != null) template.setAmount(dto.getAmount());
        if (dto.getDescription() != null) template.setDescription(dto.getDescription());
        if (dto.getFrequency() != null) {
            template.setFrequency(RecurrenceFrequency.valueOf(dto.getFrequency().toUpperCase()));
        }
        if (dto.getEndDate() != null) template.setEndDate(dto.getEndDate());
        
        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Hesab tapılmadı"));
            template.setAccount(account);
        }
        if (dto.getCategoryId() != null) {
            TransactionCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kateqoriya tapılmadı"));
            template.setCategory(category);
        }

        return convertToRecurringDto(recurringExpenseTemplateRepository.save(template));
    }

    @Override
    public List<RecurringExpenseReadDto> getRecurringExpenses(String userEmail) {
        User user = findUser(userEmail);
        return recurringExpenseTemplateRepository.findByCompanyIdOrderByCreatedAtDesc(user.getCompany().getId())
                .stream().map(this::convertToRecurringDto).collect(Collectors.toList());
    }

    @Override
    public RecurringExpenseReadDto getRecurringExpenseById(Long id, String userEmail) {
        User user = findUser(userEmail);
        RecurringExpenseTemplate template = recurringExpenseTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şablon tapılmadı"));
        if (!template.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu şablona icazəniz yoxdur");
        }
        return convertToRecurringDto(template);
    }

    @Override
    @Transactional
    public void pauseRecurringExpense(Long id, String userEmail) {
        updateRecurringExpenseStatus(id, userEmail, RecurringTemplateStatus.PAUSED);
    }

    @Override
    @Transactional
    public void resumeRecurringExpense(Long id, String userEmail) {
        updateRecurringExpenseStatus(id, userEmail, RecurringTemplateStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void cancelRecurringExpense(Long id, String userEmail) {
        updateRecurringExpenseStatus(id, userEmail, RecurringTemplateStatus.CANCELLED);
    }

    private void updateRecurringExpenseStatus(Long id, String userEmail, RecurringTemplateStatus newStatus) {
        User user = findUser(userEmail);
        RecurringExpenseTemplate template = recurringExpenseTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Şablon tapılmadı"));
        if (!template.getCompany().getId().equals(user.getCompany().getId())) {
            throw new RuntimeException("Bu şablona icazəniz yoxdur");
        }
        template.setStatus(newStatus);
        recurringExpenseTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public void executeRecurringExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpenseTemplate> activeTemplates = recurringExpenseTemplateRepository
                .findByStatusAndNextExecutionDateLessThanEqual(RecurringTemplateStatus.ACTIVE, today);

        for (RecurringExpenseTemplate template : activeTemplates) {
            if (template.getEndDate() != null && today.isAfter(template.getEndDate())) {
                template.setStatus(RecurringTemplateStatus.CANCELLED);
                recurringExpenseTemplateRepository.save(template);
                continue;
            }

            FinancialTransaction transaction = new FinancialTransaction();
            transaction.setAmount(template.getAmount());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setType(TransactionType.EXPENSE);
            transaction.setDescription(template.getDescription() != null ? 
                template.getDescription() + " (Avtomatik)" : 
                "Şablon əsasında avtomatik yaradıldı: " + template.getName());
            transaction.setAccount(template.getAccount());
            transaction.setCategory(template.getCategory());
            transaction.setCompany(template.getCompany());
            transaction.setCreatedBy(template.getCreatedBy());

            Account account = template.getAccount();
            account.setBalance(account.getBalance().subtract(template.getAmount()));
            
            transactionRepository.save(transaction);
            accountRepository.save(account);

            template.setLastExecutedAt(today);
            template.setNextExecutionDate(calculateNextExecutionDate(template.getNextExecutionDate(), template.getFrequency()));
            
            recurringExpenseTemplateRepository.save(template);
        }
    }

    private LocalDate calculateNextExecutionDate(LocalDate currentDate, RecurrenceFrequency frequency) {
        switch (frequency) {
            case DAILY: return currentDate.plusDays(1);
            case WEEKLY: return currentDate.plusWeeks(1);
            case MONTHLY: return currentDate.plusMonths(1);
            case QUARTERLY: return currentDate.plusMonths(3);
            case YEARLY: return currentDate.plusYears(1);
            default: return currentDate.plusMonths(1);
        }
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
    }

    // ——— DTO Çeviricilər ———

    private TransactionReadDto convertToReadDto(FinancialTransaction entity) {
        TransactionReadDto dto = new TransactionReadDto();
        dto.setId(entity.getId());
        dto.setAmount(entity.getAmount());
        dto.setTransactionDate(entity.getTransactionDate());
        dto.setType(entity.getType().name());
        dto.setDescription(entity.getDescription());
        dto.setAccountName(entity.getAccount().getName());
        dto.setCategoryName(entity.getCategory().getName());
        dto.setStatus("Tamamlandı");
        if (entity.getPartner() != null) {
            dto.setPartnerId(entity.getPartner().getId());
            dto.setPartnerName(entity.getPartner().getName());
        }
        if (entity.getInvoice() != null) {
            dto.setInvoiceId(entity.getInvoice().getId());
            dto.setInvoiceNumber(entity.getInvoice().getInvoiceNumber());
        }
        return dto;
    }

    private AccountReadDto convertAccountToDto(Account entity) {
        AccountReadDto dto = new AccountReadDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setBalance(entity.getBalance());
        return dto;
    }

    private CategoryReadDto convertCategoryToDto(TransactionCategory entity) {
        CategoryReadDto dto = new CategoryReadDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType().name());
        return dto;
    }

    private RecurringExpenseReadDto convertToRecurringDto(RecurringExpenseTemplate entity) {
        RecurringExpenseReadDto dto = new RecurringExpenseReadDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAmount(entity.getAmount());
        dto.setDescription(entity.getDescription());
        dto.setFrequency(entity.getFrequency().name());
        dto.setStatus(entity.getStatus().name());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setNextExecutionDate(entity.getNextExecutionDate());
        dto.setLastExecutedAt(entity.getLastExecutedAt());
        dto.setAccountId(entity.getAccount().getId());
        dto.setAccountName(entity.getAccount().getName());
        dto.setCategoryId(entity.getCategory().getId());
        dto.setCategoryName(entity.getCategory().getName());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
