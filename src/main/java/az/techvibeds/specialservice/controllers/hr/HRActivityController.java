package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.hrActivity.HRActivityCreateDto;
import az.techvibeds.specialservice.dtos.hrActivity.HRActivityResponseDto;
import az.techvibeds.specialservice.services.HRActivityService;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr-activities")
@RequiredArgsConstructor
public class HRActivityController {

    private final HRActivityService activityService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<HRActivityResponseDto> createActivity(
            @Valid @RequestBody HRActivityCreateDto request,
            Principal principal) {
        return ResponseEntity.ok(activityService.createActivity(request, principal.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<HRActivityResponseDto>> getEmployeeHistory(
            @PathVariable Long employeeId,
            Principal principal) {
        return ResponseEntity.ok(activityService.getEmployeeActivities(employeeId, principal.getName()));
    }

    @GetMapping("/feed")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<HRActivityResponseDto>> getCompanyFeed(Principal principal) {
        return ResponseEntity.ok(activityService.getCompanyActivityFeed(principal.getName()));
    }
}
