package az.techvibeds.specialservice.controllers.manufacturing;

import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseReadDto;
import az.techvibeds.specialservice.dtos.indirectExpense.IndirectExpenseUpdateDto;
import az.techvibeds.specialservice.enums.ExpenseCategory;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.IndirectExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/indirect-expense")
public class IndirectExpenseController {
    private final IndirectExpenseService indirectExpenseService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<IndirectExpenseReadDto>> create(
            @Valid @RequestBody IndirectExpenseCreateDto dto,
            Principal principal) {
        IndirectExpenseReadDto data = indirectExpenseService.create(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Expense created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN')")
    public ResponseEntity<ResponseDto<IndirectExpenseReadDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody IndirectExpenseUpdateDto dto,
            Principal principal) {
        IndirectExpenseReadDto data = indirectExpenseService.update(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Expense updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:MANUFACTURING:VIEWER', 'MODULE:MANUFACTURING:SUPER_ADMIN', 'MODULE:MANUFACTURING:ADMIN', 'MODULE:MANUFACTURING:CREATOR')")
    public ResponseEntity<ResponseDto<List<IndirectExpenseReadDto>>> getExpenses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ExpenseCategory category,
            Principal principal) {
        List<IndirectExpenseReadDto> data = indirectExpenseService.getExpenses(keyword, category, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Expenses retrieved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:MANUFACTURING:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        indirectExpenseService.delete(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Expense deleted successfully"));
    }
}
