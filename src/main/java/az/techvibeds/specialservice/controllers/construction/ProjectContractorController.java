package az.techvibeds.specialservice.controllers.construction;

import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorReadDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorUpdateDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorCreateDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ProjectContractorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/project-contractors")
@RequiredArgsConstructor
public class ProjectContractorController {

    private final ProjectContractorService contractorService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectContractorReadDto>> create(
            @Valid @RequestBody ProjectContractorCreateDto dto,
            Principal principal) {
        ProjectContractorReadDto data = contractorService.create(dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Contractor created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectContractorReadDto>> getById(@PathVariable Long id, Principal principal) {
        ProjectContractorReadDto data = contractorService.getById(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Contractor found"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProjectContractorDto>>> getContractors(
            @RequestParam(required = false) String name,
            Principal principal) {

        List<ProjectContractorDto> data = contractorService.getFilteredContractors(name, principal.getName());
        String message = data.isEmpty() ? "No contractors found" : "Contractors retrieved successfully";
        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN')")
    public ResponseEntity<ResponseDto<ProjectContractorReadDto>> update(
            @PathVariable Long id,
            @RequestBody ProjectContractorUpdateDto dto,
            Principal principal) {
        ProjectContractorReadDto data = contractorService.update(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Contractor updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        contractorService.delete(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Contractor deleted successfully"));
    }
}