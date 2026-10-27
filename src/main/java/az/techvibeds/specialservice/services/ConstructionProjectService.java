package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.construction.ConstructionStatisticsDto;
import az.techvibeds.specialservice.dtos.constructionProject.*;
import az.techvibeds.specialservice.enums.ProjectStatus;
import az.techvibeds.specialservice.models.ConstructionProject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public interface ConstructionProjectService {

    ConstructionStatisticsDto getConstructionPageStatistics(String email);

    List<ConstructionProjectDto> getProjectsByCompany(String email);

    List<ConstructionProjectDto> getFilteredProjects(ProjectStatus status, String projectName, String email);

    ConstructionProjectReadDto createConstructionProject(String email, ConstructionProjectCreateDto dto);

    ConstructionProjectReadDto updateConstructionProject(Long id, ConstructionProjectUpdateDto dto, String email);

    void deleteConstructionProject(Long id, String email);

    ConstructionProject findById(Long projectId);

    List<ProjectStatusDto> getAllStatuses();

    void updateProjectStatus(Long id, ProjectStatus status, String email);

    void updateUsedBudget(@NotNull(message = "Amount is required") @Positive(message = "Amount must be greater than zero") BigDecimal amount, Long projectId);
}
