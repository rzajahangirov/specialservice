package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.installment.*;
import az.techvibeds.specialservice.services.PaymentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payment-plans")
@RequiredArgsConstructor
public class PaymentPlanController {

    private final PaymentPlanService planService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentPlanReadDto> createPlan(@RequestBody PaymentPlanCreateDto dto, Principal principal) {
        return ResponseEntity.ok(planService.createPlan(dto, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PaymentPlanReadDto>> getPlans(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable, 
            Principal principal) {
        return ResponseEntity.ok(planService.getPlans(pageable, principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentPlanReadDto> getPlanById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(planService.getPlanById(id, principal.getName()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDashboardDto> getDashboard(Principal principal) {
        return ResponseEntity.ok(planService.getDashboard(principal.getName()));
    }

    @PostMapping("/{planId}/installments/{installmentId}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentPlanReadDto> payInstallment(
            @PathVariable Long planId, @PathVariable Long installmentId, Principal principal) {
        return ResponseEntity.ok(planService.payInstallment(planId, installmentId, principal.getName()));
    }

    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationSettingDto> getSettings(Principal principal) {
        return ResponseEntity.ok(planService.getNotificationSettings(principal.getName()));
    }

    @PutMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationSettingDto> updateSettings(@RequestBody NotificationSettingDto dto, Principal principal) {
        return ResponseEntity.ok(planService.updateNotificationSettings(dto, principal.getName()));
    }
}
