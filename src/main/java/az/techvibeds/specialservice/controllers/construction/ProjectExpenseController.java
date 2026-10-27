package az.techvibeds.specialservice.controllers.construction;

import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseReadDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseUpdateDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ProjectExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/project-expenses")
@RequiredArgsConstructor
public class ProjectExpenseController {

    private final ProjectExpenseService projectExpenseService;

    @PostMapping("/{projectId}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectExpenseReadDto>> create(
            @Valid @RequestBody ProjectExpenseCreateDto dto,
            @PathVariable Long projectId,
            Principal principal) {
        ProjectExpenseReadDto data = projectExpenseService.create(dto, principal.getName(), projectId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Expense recorded successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectExpenseReadDto>> getById(@PathVariable Long id, Principal principal) {
        ProjectExpenseReadDto data = projectExpenseService.getById(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Expense details found"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProjectExpenseDto>>> getAll(Principal principal) {
        List<ProjectExpenseDto> data = projectExpenseService.getAllByCompany(principal.getName());
        String message = data.isEmpty() ? "No expenses found" : "Expenses retrieved successfully";
        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN')")
    public ResponseEntity<ResponseDto<ProjectExpenseReadDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectExpenseUpdateDto dto,
            Principal principal) {
        ProjectExpenseReadDto data = projectExpenseService.update(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Expense updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        projectExpenseService.delete(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Expense deleted successfully"));
    }
}

