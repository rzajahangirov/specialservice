package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.deduction.DeductionCreateDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionResponseDto;
import az.techvibeds.specialservice.dtos.deduction.DeductionUpdateDto;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.services.DeductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/hr/deductions")
@RequiredArgsConstructor
public class DeductionController {

    private final DeductionService deductionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<DeductionResponseDto> createDeduction(
            @Valid @RequestBody DeductionCreateDto request,
            Principal principal) {

        log.debug("POST /hr/deductions — caller: {}", principal.getName());
        DeductionResponseDto response = deductionService.createDeduction(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<EnumResponseDto>> getDeductionTypes() {
        return ResponseEntity.ok(deductionService.getDeductionTypes());
    }

    @GetMapping("/duration-types")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<EnumResponseDto>> getDeductionDurationTypes() {
        return ResponseEntity.ok(deductionService.getDeductionDurationTypes());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<DeductionResponseDto> updateDeduction(
            @PathVariable Long id,
            @Valid @RequestBody DeductionUpdateDto request,
            Principal principal) {

        return ResponseEntity.ok(deductionService.updateDeduction(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            Principal principal) {

        deductionService.toggleDeductionStatus(id, active, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDeduction(@PathVariable Long id, Principal principal) {
        deductionService.deleteDeduction(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trash")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<DeductionResponseDto>> getDeletedDeductions(Principal principal) {
        return ResponseEntity.ok(deductionService.getDeletedDeductions(principal.getName()));
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<DeductionResponseDto> restoreDeduction(
            @PathVariable Long id,
            Principal principal) {

        return ResponseEntity.ok(deductionService.restoreDeduction(id, principal.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<DeductionResponseDto>> getEmployeeDeductions(
            @PathVariable Long employeeId, Principal principal) {
        return ResponseEntity.ok(deductionService.getEmployeeDeductions(employeeId, principal.getName()));
    }
}