package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.payroll.*;
import az.techvibeds.specialservice.services.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/hr/payrolls")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<PayrollResponseDto> generatePayroll(
            @Valid @RequestBody PayrollCreateDto request,
            Principal principal) {

        log.debug("POST /hr/payrolls/generate — caller: {}", principal.getName());
        PayrollResponseDto response = payrollService.generatePayroll(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<PayrollResponseDto> updatePayroll(
            @PathVariable Long id,
            @Valid @RequestBody PayrollUpdateDto request,
            Principal principal) {
        return ResponseEntity.ok(payrollService.updatePayroll(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/payment-status")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<Void> togglePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PayrollPaymentDto request,
            Principal principal) {

        payrollService.togglePaymentStatus(id, request, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/advance")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<PayrollResponseDto> payAdvance(
            @PathVariable Long id,
            @Valid @RequestBody az.techvibeds.specialservice.dtos.payroll.AdvancePaymentDto request,
            Principal principal) {
        
        return ResponseEntity.ok(payrollService.payAdvance(id, request, principal.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<PayrollResponseDto>> getEmployeePayrollHistory(
            @PathVariable Long employeeId, Principal principal) {
        return ResponseEntity.ok(payrollService.getEmployeePayrollHistory(employeeId, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<PayrollResponseDto>> getPayrollsByFilters(
            @RequestParam(required = false) YearMonth period,
            @RequestParam(required = false) Boolean isPaid,
            Principal principal) {
        return ResponseEntity.ok(payrollService.getPayrollsByFilters(period, isPaid, principal.getName()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<PayrollStatsDto> getPayrollStats(
            @RequestParam(required = false) YearMonth period,
            Principal principal) {
        return ResponseEntity.ok(payrollService.getPayrollStats(period, principal.getName()));
    }
}

