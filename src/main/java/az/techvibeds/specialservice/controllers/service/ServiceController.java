package az.techvibeds.specialservice.controllers.service;


import az.techvibeds.specialservice.dtos.service.*;
import az.techvibeds.specialservice.enums.ServiceStatus;
import az.techvibeds.specialservice.payloads.ApiResponse;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/descriptions")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<ResponseDto<List<ServiceDescriptionDto>>> getServiceDescriptions(Principal principal) {
        List<ServiceDescriptionDto> readDto = serviceService.getServiceDescriptions(principal.getName());
        ResponseDto<List<ServiceDescriptionDto>> responseDto = new ResponseDto<>();
        responseDto.setData(readDto);
        if (readDto.isEmpty()) {
            responseDto.setMessage("No services found");
        }else{
            responseDto.setMessage("Services found successfully");
        }

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/execution")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<ResponseDto<List<ServiceExecutionStatusDto>>> getServiceExecutionStatus(Principal principal) {
        List<ServiceExecutionStatusDto> readDto = serviceService.getServiceExecutionStatus(principal.getName());
        ResponseDto<List<ServiceExecutionStatusDto>> responseDto = new ResponseDto<>();
        responseDto.setData(readDto);
        if (readDto.isEmpty()) {
            responseDto.setMessage("No services found");
        }else{
            responseDto.setMessage("Services found successfully");
        }
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<List<String>> getServiceStatuses() {
        return ResponseEntity.ok(serviceService.getServiceStatuses());
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<List<ServiceCount>> getServiceCounts(Principal principal) {
        return ResponseEntity.ok(serviceService.getCountOfServicesByCompany(principal.getName()));
    }


    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<ServiceReadDto> createService(Principal principal,
                                                        @Valid @RequestBody ServiceCreateDto dto) {
        return ResponseEntity.ok(serviceService.createService(principal.getName(), dto));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN')")
    public ResponseEntity<ServiceReadDto> updateService(@Valid @RequestBody ServiceUpdateDto dto, Principal principal) {
        return ResponseEntity.ok(serviceService.updateService(dto, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:SERVICE:SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> deleteService(@PathVariable Long id, Principal principal) {
        serviceService.delete(id, principal.getName());
        return ResponseEntity.ok(new ApiResponse("Service deleted", true));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<ServiceReadDto> payService(@PathVariable Long id, @Valid @RequestBody ServicePaymentDto dto, Principal principal) {
        return ResponseEntity.ok(serviceService.payService(id, dto, principal.getName()));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MODULE:SERVICE:VIEWER', 'MODULE:SERVICE:SUPER_ADMIN', 'MODULE:SERVICE:ADMIN', 'MODULE:SERVICE:CREATOR')")
    public ResponseEntity<List<ServiceExecutionStatusDto>> getServiceExceptionsFiltered(Principal principal, @RequestParam("filter") ServiceStatus status, @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(serviceService.getServiceByFiltered(principal.getName(), status, keyword));
    }

}
