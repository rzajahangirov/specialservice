package az.techvibeds.specialservice.controllers.construction;

import az.techvibeds.specialservice.dtos.construction.ConstructionStatisticsDto;
import az.techvibeds.specialservice.dtos.constructionProject.*;
import az.techvibeds.specialservice.enums.ProjectStatus;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ConstructionProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/construction")
public class ConstructionController {

    private final ConstructionProjectService projectService;

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ConstructionStatisticsDto>> getSyntaxData(Principal principal) {
        ConstructionStatisticsDto data = projectService.getConstructionPageStatistics(principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(data, "Syntax data retrieved successfully"));
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ConstructionProjectDto>>> getAllProjects(Principal principal) {
        List<ConstructionProjectDto> data = projectService.getProjectsByCompany(principal.getName());
        String message = data.isEmpty() ? "No construction projects found" : "Projects retrieved successfully";
        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ConstructionProjectDto>>> filterProjects(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String projectName,
            Principal principal) {

        List<ConstructionProjectDto> data = projectService.getFilteredProjects(status, projectName, principal.getName());
        String message = data.isEmpty() ? "No projects match the given criteria" : "Filtered projects retrieved";
        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ConstructionProjectReadDto>> createProject(
            @RequestBody ConstructionProjectCreateDto dto,
            Principal principal) {

        ConstructionProjectReadDto created = projectService.createConstructionProject(principal.getName(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(created, "Project created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN')")
    public ResponseEntity<ResponseDto<ConstructionProjectReadDto>> updateProject(
            @PathVariable Long id,
            @RequestBody ConstructionProjectUpdateDto dto,
            Principal principal) {

        ConstructionProjectReadDto updated = projectService.updateConstructionProject(id, dto, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(updated, "Project updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN')")
    public ResponseEntity<ResponseDto<Void>> deleteProject(@PathVariable Long id, Principal principal) {
        projectService.deleteConstructionProject(id, principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Project deleted successfully"));
    }

    @GetMapping("/project-statuses")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ProjectStatusDto>>> getStatuses() {
        List<ProjectStatusDto> data = projectService.getAllStatuses();
        return ResponseEntity.ok(new ResponseDto<>(data, "Project statuses retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN')")
    public ResponseEntity<ResponseDto<Void>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProjectStatusUpdateDto dto,
            Principal principal) {

        projectService.updateProjectStatus(id, dto.getStatus(), principal.getName());
        return ResponseEntity.ok(new ResponseDto<>(null, "Project status updated to " + dto.getStatus()));
    }
}