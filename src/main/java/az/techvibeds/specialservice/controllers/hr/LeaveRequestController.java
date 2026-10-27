package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestCreateDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestResponseDto;
import az.techvibeds.specialservice.dtos.leaveRequest.LeaveRequestUpdateDto;
import az.techvibeds.specialservice.enums.ActivityStatus;
import az.techvibeds.specialservice.enums.LeaveType;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.services.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestController {

    private final LeaveRequestService leaveService;

    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<EnumResponseDto>> getLeaveTypes() {
        return ResponseEntity.ok(leaveService.getLeaveTypes());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<LeaveRequestResponseDto> createRequest(
            @Valid @RequestBody LeaveRequestCreateDto request,
            Principal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveService.createRequest(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<LeaveRequestResponseDto> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody LeaveRequestUpdateDto request,
            Principal principal) {

        return ResponseEntity.ok(leaveService.updateRequest(id, request, principal.getName()));
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<EnumResponseDto>> getLeaveStatuses() {
        return ResponseEntity.ok(leaveService.getLeaveStatuses());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeaveRequestResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestParam ActivityStatus status,
            Principal principal) {

        return ResponseEntity.ok(leaveService.updateLeaveStatus(id, status, principal.getName()));
    }

    @GetMapping("/company-requests")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<LeaveRequestResponseDto>> getAllCompanyRequests(
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) LeaveType type,
            Principal principal) {

        return ResponseEntity.ok(leaveService.getAllCompanyLeaveRequests(status, type, principal.getName()));
    }
}
