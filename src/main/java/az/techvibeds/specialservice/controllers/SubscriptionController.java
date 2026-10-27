package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.installment.SubscriptionStatusDto;
import az.techvibeds.specialservice.dtos.subscription.SubscribeRequestDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionStatusDto> subscribe(@RequestBody SubscribeRequestDto dto, Principal principal) {
        return ResponseEntity.ok(subscriptionService.subscribe(dto, principal.getName()));
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionStatusDto> getStatus(Principal principal) {
        return ResponseEntity.ok(subscriptionService.getStatus(principal.getName()));
    }

    // ═══════════════════════════════════════════════════════════
    //  GLOBAL_ADMIN: Manual Company Freeze / Unfreeze
    // ═══════════════════════════════════════════════════════════

    /**
     * Manually freeze (deactivate) a company's access.
     * No data is deleted — modules are deactivated, user roles are preserved.
     */
    @PostMapping("/freeze/{companyId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ApiResponse> freezeCompany(@PathVariable Long companyId) {
        subscriptionService.freezeCompany(companyId);
        return ResponseEntity.ok(new ApiResponse("Company access has been frozen successfully.", true));
    }

    /**
     * Manually unfreeze (reactivate) a company's access.
     * All modules are reactivated, existing user roles are restored.
     */
    @PostMapping("/unfreeze/{companyId}")
    @PreAuthorize("hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<ApiResponse> unfreezeCompany(@PathVariable Long companyId) {
        subscriptionService.unfreezeCompany(companyId);
        return ResponseEntity.ok(new ApiResponse("Company access has been restored successfully.", true));
    }
}
