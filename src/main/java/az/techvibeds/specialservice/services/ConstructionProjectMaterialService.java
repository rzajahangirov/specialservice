package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialCreateDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialReadDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialUseDto;

import java.util.List;

public interface ConstructionProjectMaterialService {
    ConstructionProjectMaterialReadDto addRequiredMaterial(
            Long projectId, ConstructionProjectMaterialCreateDto dto, String email);

    ConstructionProjectMaterialReadDto useMaterial(
            Long projectId, ConstructionProjectMaterialUseDto dto, String email);

    List<ConstructionProjectMaterialReadDto> getProjectMaterials(Long projectId, String email);
}
