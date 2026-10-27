package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.finance.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface FinanceService {

    // ——— Dashboard ———
    FinanceSummaryDto getFinanceSummary(String userEmail);
    FinanceSummaryDto getFinanceSummaryForDateRange(String userEmail, LocalDate startDate, LocalDate endDate);

    // ——— Tranzaksiyalar ———
    TransactionReadDto createTransaction(TransactionCreateDto dto, String userEmail);
    Page<TransactionReadDto> getTransactions(Pageable pageable, String userEmail);
    TransactionReadDto getTransactionById(Long id, String userEmail);
    void deleteTransaction(Long id, String userEmail);

    // ——— Hesablar (Account) ———
    AccountReadDto createAccount(AccountCreateDto dto, String userEmail);
    List<AccountReadDto> getAccounts(String userEmail);
    AccountReadDto updateAccount(Long id, AccountUpdateDto dto, String userEmail);
    void deleteAccount(Long id, String userEmail);

    // ——— Kateqoriyalar ———
    CategoryReadDto createCategory(CategoryCreateDto dto, String userEmail);
    List<CategoryReadDto> getCategories(String userEmail);

    // ——— Hesabatlar ———
    FinanceSummaryDto getBalanceSheet(String userEmail);
    FinanceSummaryDto getProfitLoss(String userEmail);
    FinanceSummaryDto getCashFlow(String userEmail);

    // ——— Təkrarlanan Xərc Şablonları ———
    RecurringExpenseReadDto createRecurringExpense(RecurringExpenseCreateDto dto, String userEmail);
    RecurringExpenseReadDto updateRecurringExpense(Long id, RecurringExpenseUpdateDto dto, String userEmail);
    List<RecurringExpenseReadDto> getRecurringExpenses(String userEmail);
    RecurringExpenseReadDto getRecurringExpenseById(Long id, String userEmail);
    void pauseRecurringExpense(Long id, String userEmail);
    void resumeRecurringExpense(Long id, String userEmail);
    void cancelRecurringExpense(Long id, String userEmail);
    void executeRecurringExpenses();
}
