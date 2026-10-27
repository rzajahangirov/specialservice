package az.techvibeds.specialservice.controllers.hr;

import az.techvibeds.specialservice.dtos.hrEmployee.*;
import az.techvibeds.specialservice.enums.EmploymentStatus;
import az.techvibeds.specialservice.payloads.EnumResponseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.services.HREmployeeService;
import jakarta.servlet.http.HttpServletResponse;
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
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/hr/employee")
public class HREmployeeController {
    private final HREmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:INVENTORY:SUPER_ADMIN', 'MODULE:INVENTORY:ADMIN', 'MODULE:INVENTORY:CREATOR')")
    public ResponseEntity<HREmployeeResponseDto> create(@Valid @RequestBody HREmployeeCreateDto request, Principal principal) {
        HREmployeeResponseDto response = employeeService.createEmployee(request, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<List<HREmployeeListDto>>> getAll(Principal principal
            , @RequestParam(required = false) EmploymentStatus status) {
        return ResponseEntity.ok(employeeService.getAllEmployees(principal.getName(), status));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<HREmployeeDetailDto>> getDetail(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(employeeService.getEmployeeDetail(id, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN')")
    public ResponseEntity<ResponseDto<HREmployeeResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody HREmployeeUpdateDto updateDTO,
            Principal principal) {

        HREmployeeResponseDto response = employeeService.updateEmployee(id, updateDTO, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(response, "Employee updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseDto<Void>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto statusUpdateDTO,
            Principal principal) {

        employeeService.updateStatus(id, statusUpdateDTO.getStatus(), principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Employee status updated successfully"));
    }
    @GetMapping("/statuses")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<List<EnumResponseDto>>> getStatuses() {
        return ResponseEntity.ok(employeeService.getEmploymentStatuses());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN')")
    public ResponseEntity<Void> softDeleteEmployee(
            @PathVariable Long id,
            Principal principal) {

        log.debug("DELETE /hr/employees/{} — user: {}", id, principal.getName());

        employeeService.softDeleteEmployee(id, principal.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<List<DeletedEmployeeResponseDto>> getDeletedEmployees(Principal principal) {
        log.debug("GET /hr/employees/deleted — user: {}", principal.getName());

        List<DeletedEmployeeResponseDto> response =
                employeeService.getDeletedEmployeesByCompany(principal.getName());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestoredEmployeeResponseDto> restoreEmployee(
            @PathVariable Long id,
            Principal principal) {

        log.debug("PATCH /hr/employees/{}/restore — user: {}", id, principal.getName());

        RestoredEmployeeResponseDto response =
                employeeService.restoreEmployee(id, principal.getName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<HRDashboardStatsDto> getDashboardStats(Principal principal) {
        return ResponseEntity.ok(employeeService.getDashboardStats(principal.getName()));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('MODULE:HR:VIEWER', 'MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public void exportEmployees(HttpServletResponse response, Principal principal) {
        employeeService.exportEmployeesToExcel(response, principal.getName());
    }

    @PostMapping(value = "/{id}/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ResponseDto<String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") org.springframework.web.multipart.MultipartFile image,
            Principal principal) throws java.io.IOException {
        
        String fileName = employeeService.uploadProfileImage(id, image, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(fileName, "Image uploaded successfully"));
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MODULE:HR:SUPER_ADMIN', 'MODULE:HR:ADMIN', 'MODULE:HR:CREATOR')")
    public ResponseEntity<ApiResponse> importEmployees(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Principal principal) throws Exception {
        employeeService.importEmployeesFromExcel(file, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Excel faylı uğurla idxal edildi!", true));
    }
}


