package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.projectStage.ProjectStageCreateDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageReadDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageUpdateDto;
import az.techvibeds.specialservice.enums.StageStatus;
import az.techvibeds.specialservice.models.ConstructionProject;
import az.techvibeds.specialservice.models.ProjectStage;
import az.techvibeds.specialservice.repositories.ProjectStageRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ConstructionProjectService;
import az.techvibeds.specialservice.services.ProjectStageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectStageServiceImpl implements ProjectStageService {
    private final ProjectStageRepository projectStageRepository;
    private final ConstructionProjectService projectService;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ProjectStageReadDto create(Long projectId, ProjectStageCreateDto dto, String email) {
        log.info("Creating stage for project ID: {} by user: {}", projectId, email);

        ConstructionProject project = projectService.findById(projectId);
        validateProjectOwnership(project, email);

        ProjectStage stage = modelMapper.map(dto, ProjectStage.class);
        stage.setProject(project);

        ProjectStage saved = projectStageRepository.save(stage);
        return mapToRead(saved);
    }

    @Override
    public List<ProjectStageReadDto> getAllByProject(Long projectId, String email) {
        log.info("Fetching stages for project ID: {}", projectId);


        ConstructionProject project = projectService.findById(projectId);
        validateProjectOwnership(project, email);

        return projectStageRepository.findAllByProjectId(projectId)
                .stream()
                .map(this::mapToRead)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectStageReadDto update(Long id, ProjectStageUpdateDto dto, String email) {
        log.info("Updating stage ID: {} by user: {}", id, email);
        ProjectStage stage = findAndValidateOwner(id, email);

        stage.setName(dto.getName());
        stage.setStageNumber(dto.getStageNumber());
        stage.setProgressPercentage(dto.getProgressPercentage());

        stage.setStatus(dto.getStatus());

        return mapToRead(projectStageRepository.save(stage));
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        log.info("Deleting stage ID: {} by user: {}", id, email);
        ProjectStage stage = findAndValidateOwner(id, email);
        projectStageRepository.delete(stage);
    }

    @Override
    public List<ProjectStageDto> getActiveStages(String email) {
        Long companyId = companyService.findByUserEmail(email).getId();
        return projectStageRepository.findActiveStagesByCompanyId(companyId, StageStatus.ACTIVE)
                .stream()
                .map(s -> {
                    ProjectStageDto dto = modelMapper.map(s, ProjectStageDto.class);
                    dto.setProjectName(s.getProject().getName());
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    public ProjectStageReadDto getById(Long id, String email) {
        log.info("Fetching project stage details for ID: {} by user: {}", id, email);

        ProjectStage stage = findAndValidateOwner(id, email);

        return mapToRead(stage);
    }

    private void validateProjectOwnership(ConstructionProject project, String email) {
        Long userCompanyId = companyService.findByUserEmail(email).getId();
        if (!project.getCompany().getId().equals(userCompanyId)) {
            log.error("Access Denied for project ID: {} by user: {}", project.getId(), email);
            throw new AccessDeniedException("Access denied to this project");
        }
    }

    private ProjectStage findAndValidateOwner(Long id, String email) {
        ProjectStage stage = projectStageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stage not found"));
        validateProjectOwnership(stage.getProject(), email);
        return stage;
    }

    private ProjectStageReadDto mapToRead(ProjectStage projectStage) {
        ProjectStageReadDto readDto = modelMapper.map(projectStage, ProjectStageReadDto.class);
        if (projectStage.getStatus() != null) {
            readDto.setStagesStatus(projectStage.getStatus().name());
        }
        return readDto;
    }
}
