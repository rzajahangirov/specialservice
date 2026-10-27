package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.projectStage.ProjectStageCreateDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageReadDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageUpdateDto;

import java.util.List;

public interface ProjectStageService {
    ProjectStageReadDto create(Long projectId, ProjectStageCreateDto dto, String email);

    List<ProjectStageReadDto> getAllByProject(Long projectId, String userEmail);

    ProjectStageReadDto update(Long id, ProjectStageUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    List<ProjectStageDto> getActiveStages(String email);

    ProjectStageReadDto getById(Long id, String name);
}
