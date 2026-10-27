package az.techvibeds.specialservice.controllers.construction;

import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialCreateDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialReadDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialUseDto;
import az.techvibeds.specialservice.payloads.ResponseDto;
import az.techvibeds.specialservice.services.ConstructionProjectMaterialService;
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
@RequestMapping("/api/v1/construction/projects/{projectId}/materials")
public class ConstructionProjectMaterialController {

    private final ConstructionProjectMaterialService materialService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ConstructionProjectMaterialReadDto>> addRequiredMaterial(
            @PathVariable Long projectId,
            @Valid @RequestBody ConstructionProjectMaterialCreateDto dto,
            Principal principal) {

        ConstructionProjectMaterialReadDto data =
                materialService.addRequiredMaterial(projectId, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Project material added successfully"));
    }

    @PostMapping("/use")
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<ConstructionProjectMaterialReadDto>> useMaterial(
            @PathVariable Long projectId,
            @Valid @RequestBody ConstructionProjectMaterialUseDto dto,
            Principal principal) {

        ConstructionProjectMaterialReadDto data =
                materialService.useMaterial(projectId, dto, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(data, "Project material used and invoice created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MODULE:CONSTRUCTION:VIEWER', 'MODULE:CONSTRUCTION:SUPER_ADMIN', 'MODULE:CONSTRUCTION:ADMIN', 'MODULE:CONSTRUCTION:CREATOR')")
    public ResponseEntity<ResponseDto<List<ConstructionProjectMaterialReadDto>>> getProjectMaterials(
            @PathVariable Long projectId,
            Principal principal) {

        List<ConstructionProjectMaterialReadDto> data =
                materialService.getProjectMaterials(projectId, principal.getName());
        String message = data.isEmpty() ? "No project materials found" : "Project materials retrieved successfully";
        return ResponseEntity.ok(new ResponseDto<>(data, message));
    }
}
