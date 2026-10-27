package az.techvibeds.specialservice.controllers.finance;

import az.techvibeds.specialservice.dtos.finance.*;
import az.techvibeds.specialservice.services.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService financeService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanceSummaryDto> getSummary(Principal principal) {
        return ResponseEntity.ok(financeService.getFinanceSummary(principal.getName()));
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TransactionReadDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        // Valid properties for sorting: id, amount, transactionDate, type, description
        List<String> allowedSortFields = List.of("id", "amount", "transactionDate", "type", "description");
        String finalSortBy = allowedSortFields.contains(sortBy) ? sortBy : "transactionDate";

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(finalSortBy).ascending()
                : Sort.by(finalSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TransactionReadDto> transactions = financeService.getTransactions(pageable, principal.getName());
        return ResponseEntity.ok(transactions);
    }


    @PostMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionReadDto> createTransaction(
            @RequestBody TransactionCreateDto dto,
            Principal principal) {
        TransactionReadDto created = financeService.createTransaction(dto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/transactions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransactionReadDto> getTransactionById(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(financeService.getTransactionById(id, principal.getName()));
    }

    @DeleteMapping("/transactions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id, Principal principal) {
        financeService.deleteTransaction(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/income-expense-chart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MonthlySummaryDto>> getIncomeExpenseChart(Principal principal) {
        FinanceSummaryDto summary = financeService.getFinanceSummary(principal.getName());
        return ResponseEntity.ok(summary.getMonthlySummaries());
    }

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryReadDto>> getCategories(Principal principal) {
        return ResponseEntity.ok(financeService.getCategories(principal.getName()));
    }

    @PostMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryReadDto> createCategory(
            @RequestBody CategoryCreateDto dto,
            Principal principal) {
        CategoryReadDto created = financeService.createCategory(dto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AccountReadDto>> getAccounts(Principal principal) {
        return ResponseEntity.ok(financeService.getAccounts(principal.getName()));
    }

    @PostMapping("/accounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountReadDto> createAccount(
            @RequestBody AccountCreateDto dto,
            Principal principal) {
        AccountReadDto created = financeService.createAccount(dto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:FINANCE:SUPER_ADMIN', 'MODULE:FINANCE:ADMIN')")
    public ResponseEntity<AccountReadDto> updateAccount(
            @PathVariable Long id,
            @RequestBody AccountUpdateDto dto,
            Principal principal) {
        return ResponseEntity.ok(financeService.updateAccount(id, dto, principal.getName()));
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('MODULE:FINANCE:SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            Principal principal) {
        financeService.deleteAccount(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════
    //  TƏKRARLANAN XƏRC ŞABLONLARI
    // ═══════════════════════════════════════════

    @PostMapping("/recurring-expenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringExpenseReadDto> createRecurringExpense(
            @RequestBody RecurringExpenseCreateDto dto,
            Principal principal) {
        RecurringExpenseReadDto created = financeService.createRecurringExpense(dto, principal.getName());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/recurring-expenses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringExpenseReadDto> updateRecurringExpense(
            @PathVariable Long id,
            @RequestBody RecurringExpenseUpdateDto dto,
            Principal principal) {
        RecurringExpenseReadDto updated = financeService.updateRecurringExpense(id, dto, principal.getName());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/recurring-expenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RecurringExpenseReadDto>> getRecurringExpenses(Principal principal) {
        return ResponseEntity.ok(financeService.getRecurringExpenses(principal.getName()));
    }

    @GetMapping("/recurring-expenses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecurringExpenseReadDto> getRecurringExpenseById(
            @PathVariable Long id,
            Principal principal) {
        return ResponseEntity.ok(financeService.getRecurringExpenseById(id, principal.getName()));
    }

    @PostMapping("/recurring-expenses/{id}/pause")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> pauseRecurringExpense(@PathVariable Long id, Principal principal) {
        financeService.pauseRecurringExpense(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recurring-expenses/{id}/resume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> resumeRecurringExpense(@PathVariable Long id, Principal principal) {
        financeService.resumeRecurringExpense(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recurring-expenses/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelRecurringExpense(@PathVariable Long id, Principal principal) {
        financeService.cancelRecurringExpense(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
