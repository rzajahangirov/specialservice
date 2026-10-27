package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.payroll.PayrollCreateDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollResponseDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollUpdateDto;
import az.techvibeds.specialservice.dtos.payroll.PayrollStatsDto;
import az.techvibeds.specialservice.exceptions.EmployeeNotFoundException;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.*;
import az.techvibeds.specialservice.repositories.ContractRepository;
import az.techvibeds.specialservice.repositories.DeductionRepository;
import az.techvibeds.specialservice.repositories.HREmployeeRepository;
import az.techvibeds.specialservice.repositories.PayrollRepository;
import az.techvibeds.specialservice.services.PayrollService;
import az.techvibeds.specialservice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final HREmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final DeductionRepository deductionRepository;
    private final az.techvibeds.specialservice.repositories.AccountRepository accountRepository;
    private final az.techvibeds.specialservice.repositories.FinancialTransactionRepository transactionRepository;
    private final az.techvibeds.specialservice.repositories.TransactionCategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @Override
    @Transactional
    public PayrollResponseDto generatePayroll(PayrollCreateDto request, String callerEmail) {
        log.info("Generating manual payroll: employeeId={}, period={}, callerEmail={}",
                request.getEmployeeId(), request.getPeriod(), callerEmail);

        User caller = userService.findByEmail(callerEmail);

        HREmployee employee = employeeRepository.findByIdAndDeletedFalse(request.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(request.getEmployeeId()));

        validateSameCompany(caller, employee);

        if (payrollRepository.existsByEmployeeIdAndPeriod(employee.getId(), request.getPeriod())) {
            throw new IllegalStateException("Payroll record already exists for this period.");
        }

        Contract activeContract = contractRepository
                .findByEmployeeIdAndIsActiveTrue(employee.getId())
                .orElseThrow(() -> new IllegalStateException("No active contract found for this employee."));

        BigDecimal baseSalary = activeContract.getSalary();

        LocalDate periodDate = request.getPeriod().atDay(1);
        List<Deduction> activeDeductions =
                deductionRepository.findActiveDeductionsForPeriod(employee.getId(), periodDate);

        BigDecimal totalDeductions = activeDeductions.stream()
                .map(Deduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setPeriod(request.getPeriod());
        payroll.setBaseSalary(baseSalary);
        payroll.setTotalDeductions(totalDeductions);
        payroll.setPaid(false);

        BigDecimal bonus = request.getBonusAmount() != null ? request.getBonusAmount() : BigDecimal.ZERO;
        BigDecimal tax = request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO;

        payroll.setBonusAmount(bonus);
        payroll.setTaxAmount(tax);

        Payroll saved = payrollRepository.save(payroll);

        log.info("Payroll successfully saved: id={}, netSalary={}", saved.getId(), saved.getNetSalary());

        // 6. Response Mapping
        PayrollResponseDto response = modelMapper.map(saved, PayrollResponseDto.class);
        response.setEmployeeId(employee.getId());
        response.setEmployeeFullName(employee.getFirstName() + " " + employee.getLastName());

        return response;
    }

    @Override
    @Transactional
    public PayrollResponseDto updatePayroll(Long id, PayrollUpdateDto request, String callerEmail) {
        log.info("Update attempt for payrollId={} by user={}", id, callerEmail);

        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", id));

        User caller = userService.findByEmail(callerEmail);
        validateSameCompany(caller, payroll.getEmployee());

        if (payroll.isPaid()) {
            log.warn("Security Alert: User {} tried to update a PAID payroll id={}", callerEmail, id);
            throw new IllegalStateException("Cannot update payroll after it has been marked as paid.");
        }

        if (request.getBonusAmount() != null) payroll.setBonusAmount(request.getBonusAmount());
        if (request.getTaxAmount() != null) payroll.setTaxAmount(request.getTaxAmount());
        if (request.getPeriod() != null) payroll.setPeriod(request.getPeriod());
        if (request.getAdvanceAmount() != null) payroll.setAdvanceAmount(request.getAdvanceAmount());
        if (request.getDebtAmount() != null) payroll.setDebtAmount(request.getDebtAmount());


        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll id={} updated successfully", id);

        PayrollResponseDto response = modelMapper.map(saved, PayrollResponseDto.class);
        if (saved.getEmployee() != null) {
            response.setEmployeeId(saved.getEmployee().getId());
            response.setEmployeeFullName(saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName());
        }
        return response;
    }

    @Override
    @Transactional
    public void togglePaymentStatus(Long id, az.techvibeds.specialservice.dtos.payroll.PayrollPaymentDto request, String callerEmail) {
        log.info("Changing payment status for payrollId={} to {} by user={}", id, request.isPaid(), callerEmail);

        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", id));

        User caller = userService.findByEmail(callerEmail);
        validateSameCompany(caller, payroll.getEmployee());

        Company company = payroll.getEmployee().getCompany();

        if (request.isPaid() && !payroll.isPaid()) {
            // Əməkhaqqı ödənişi edilir
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Kassa/Bank hesabı tapılmadı"));

            if (company != null && !account.getCompany().getId().equals(company.getId())) {
                throw new AccessDeniedException("Bu hesab başqa şirkətə aiddir!");
            }

            TransactionCategory category = getOrCreateCategory(company, "Əməkhaqqı");

            FinancialTransaction transaction = new FinancialTransaction();
            transaction.setAmount(payroll.getNetSalary());
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transaction.setType(az.techvibeds.specialservice.enums.TransactionType.EXPENSE);
            transaction.setDescription("Əməkhaqqı ödənişi: " + payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName() + " (" + payroll.getPeriod().toString() + ")");
            transaction.setAccount(account);
            transaction.setCategory(category);
            transaction.setCompany(company);
            transaction.setCreatedBy(caller);
            transaction.setPayroll(payroll);

            account.setBalance(account.getBalance().subtract(payroll.getNetSalary()));
            
            transactionRepository.save(transaction);
            accountRepository.save(account);

            payroll.setPaid(true);

        } else if (!request.isPaid() && payroll.isPaid()) {
            // Ödəniş ləğv edilir
            List<FinancialTransaction> transactions = transactionRepository.findAll().stream()
                    .filter(t -> t.getPayroll() != null && t.getPayroll().getId().equals(payroll.getId()) && !t.getIsDeleted())
                    .toList();
            
            for (FinancialTransaction tx : transactions) {
                // Əgər tranzaksiya maaş üçündürsə ləğv edirik (avansları silmirik)
                if (tx.getDescription() != null && tx.getDescription().startsWith("Əməkhaqqı ödənişi")) {
                    Account account = tx.getAccount();
                    account.setBalance(account.getBalance().add(tx.getAmount()));
                    accountRepository.save(account);
                    
                    tx.setIsDeleted(true);
                    transactionRepository.save(tx);
                }
            }
            payroll.setPaid(false);
        }

        payrollRepository.save(payroll);
        log.info("Payroll id={} payment status set to {}", id, request.isPaid());
    }

    @Override
    @Transactional
    public PayrollResponseDto payAdvance(Long id, az.techvibeds.specialservice.dtos.payroll.AdvancePaymentDto request, String callerEmail) {
        log.info("Advance payment for payrollId={} amount={} by user={}", id, request.getAmount(), callerEmail);

        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", id));

        if (payroll.isPaid()) {
            throw new IllegalStateException("Cannot pay advance for an already paid payroll.");
        }

        User caller = userService.findByEmail(callerEmail);
        validateSameCompany(caller, payroll.getEmployee());

        Company company = payroll.getEmployee().getCompany();

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Kassa/Bank hesabı tapılmadı"));

        if (company != null && !account.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("Bu hesab başqa şirkətə aiddir!");
        }

        TransactionCategory category = getOrCreateCategory(company, "Avans");

        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(java.time.LocalDateTime.now());
        transaction.setType(az.techvibeds.specialservice.enums.TransactionType.EXPENSE);
        transaction.setDescription("Avans ödənişi: " + payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName() + " (" + payroll.getPeriod().toString() + ")");
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setCompany(company);
        transaction.setCreatedBy(caller);
        transaction.setPayroll(payroll);

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        
        transactionRepository.save(transaction);
        accountRepository.save(account);

        BigDecimal currentAdvance = payroll.getAdvanceAmount() != null ? payroll.getAdvanceAmount() : BigDecimal.ZERO;
        payroll.setAdvanceAmount(currentAdvance.add(request.getAmount()));
        
        Payroll saved = payrollRepository.save(payroll);

        PayrollResponseDto response = modelMapper.map(saved, PayrollResponseDto.class);
        if (saved.getEmployee() != null) {
            response.setEmployeeId(saved.getEmployee().getId());
            response.setEmployeeFullName(saved.getEmployee().getFirstName() + " " + saved.getEmployee().getLastName());
        }
        return response;
    }

    private TransactionCategory getOrCreateCategory(Company company, String categoryName) {
        Long companyId = company != null ? company.getId() : null;
        return categoryRepository.findByCompanyIdAndTypeAndName(companyId, az.techvibeds.specialservice.enums.TransactionType.EXPENSE, categoryName)
                .orElseGet(() -> {
                    TransactionCategory newCategory = new TransactionCategory();
                    newCategory.setName(categoryName);
                    newCategory.setType(az.techvibeds.specialservice.enums.TransactionType.EXPENSE);
                    newCategory.setCompany(company);
                    return categoryRepository.save(newCategory);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponseDto> getEmployeePayrollHistory(Long employeeId, String callerEmail) {
        log.info("Fetching payroll history for employeeId={} by user={}", employeeId, callerEmail);

        HREmployee employee = employeeRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));

        User caller = userService.findByEmail(callerEmail);
        validateSameCompany(caller, employee);

        return payrollRepository.findAllByEmployeeIdOrderByPeriodDesc(employeeId).stream()
                .map(p -> {
                    PayrollResponseDto dto = modelMapper.map(p, PayrollResponseDto.class);
                    if (p.getEmployee() != null) {
                        dto.setEmployeeId(p.getEmployee().getId());
                        dto.setEmployeeFullName(p.getEmployee().getFirstName() + " " + p.getEmployee().getLastName());
                    }
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponseDto> getPayrollsByFilters(YearMonth period, Boolean isPaid, String callerEmail) {
        log.info("Filtering payrolls for period={}, isPaid={} by user={}", period, isPaid, callerEmail);
 
        User caller = userService.findByEmail(callerEmail);
        List<Payroll> payrolls;
        
        if (caller.getCompany() == null) {
            // Super Admin
            payrolls = payrollRepository.findAll().stream()
                    .filter(p -> (period == null || p.getPeriod().equals(period)) && (isPaid == null || p.isPaid() == isPaid))
                    .toList();
        } else {
            payrolls = payrollRepository.findAllByFilters(caller.getCompany().getId(), period, isPaid);
        }

        return payrolls.stream()
                .map(p -> {
                    PayrollResponseDto dto = modelMapper.map(p, PayrollResponseDto.class);
                    if (p.getEmployee() != null) {
                        dto.setEmployeeId(p.getEmployee().getId());
                        dto.setEmployeeFullName(p.getEmployee().getFirstName() + " " + p.getEmployee().getLastName());
                    }
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollStatsDto getPayrollStats(YearMonth period, String callerEmail) {
        log.info("Calculating payroll stats for period={} by user={}", period, callerEmail);

        User caller = userService.findByEmail(callerEmail);
        YearMonth filterPeriod = period != null ? period : YearMonth.now();
        List<Payroll> payrolls;

        if (caller.getCompany() == null) {
            // Super Admin
            payrolls = payrollRepository.findAll().stream()
                    .filter(p -> p.getPeriod().equals(filterPeriod))
                    .toList();
        } else {
            payrolls = payrollRepository.findAllByFilters(caller.getCompany().getId(), filterPeriod, null);
        }

        BigDecimal totalExpense = BigDecimal.ZERO;
        long recordCount = payrolls.size();

        for (Payroll p : payrolls) {
            if (p.getNetSalary() != null) {
                totalExpense = totalExpense.add(p.getNetSalary());
            }
        }

        BigDecimal averageSalary = BigDecimal.ZERO;
        if (recordCount > 0) {
            averageSalary = totalExpense.divide(BigDecimal.valueOf(recordCount), 2, java.math.RoundingMode.HALF_UP);
        }

        return PayrollStatsDto.builder()
                .totalExpense(totalExpense)
                .averageSalary(averageSalary)
                .recordCount(recordCount)
                .build();
    }

    private void validateSameCompany(User caller, HREmployee target) {
        if (caller.getCompany() == null) {
            // Super Admin
            return;
        }
        if (target.getCompany() == null ||
                !caller.getCompany().getId().equals(target.getCompany().getId())) {
            throw new AccessDeniedException("Unauthorized: Company access mismatch.");
        }
    }
}