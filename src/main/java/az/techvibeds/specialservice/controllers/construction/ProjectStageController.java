package az.techvibeds.specialservice.controllers.construction;

import az.techvibeds.specialservice.dtos.projectStage.ProjectStageCreateDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageReadDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageUpdateDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ProjectStageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/project-stages")
@RequiredArgsConstructor
public class ProjectStageController {

    private final ProjectStageService projectStageService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProjectStageDto>>> getActiveStages(Principal principal) {
        List<ProjectStageDto> data = projectStageService.getActiveStages(principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Active stages retrieved"));
    }

    @PostMapping("/{projectId}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectStageReadDto>> create(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectStageCreateDto dto,
            Principal principal) {
        ProjectStageReadDto data = projectStageService.create(projectId, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Stage created successfully"));
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProjectStageReadDto>>> getAllByProject(
            @PathVariable Long projectId,
            Principal principal) {
        List<ProjectStageReadDto> data = projectStageService.getAllByProject(projectId, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Project stages retrieved"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ProjectStageReadDto>> getById(@PathVariable Long id, Principal principal) {
        ProjectStageReadDto data = projectStageService.getById(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Stage details found"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN')")
    public ResponseEntity<ResponseDto<ProjectStageReadDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStageUpdateDto dto,
            Principal principal) {
        ProjectStageReadDto data = projectStageService.update(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Stage updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable Long id, Principal principal) {
        projectStageService.delete(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Stage deleted successfully"));
    }
}


