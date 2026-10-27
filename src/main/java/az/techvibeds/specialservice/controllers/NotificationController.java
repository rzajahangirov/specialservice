package az.techvibeds.specialservice.controllers;

import az.techvibeds.specialservice.dtos.installment.NotificationSettingDto;
import az.techvibeds.specialservice.services.PaymentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PaymentPlanService planService;

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
