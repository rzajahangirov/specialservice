package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.construction.ConstructionStatisticsDto;
import az.techvibeds.specialservice.dtos.constructionMaterial.ConstructionProjectMaterialReadDto;
import az.techvibeds.specialservice.dtos.constructionProject.*;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseReadDto;
import az.techvibeds.specialservice.dtos.projectStage.ProjectStageReadDto;
import az.techvibeds.specialservice.enums.ProjectStatus;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.ConstructionProject;
import az.techvibeds.specialservice.repositories.ConstructionProjectRepository;
import az.techvibeds.specialservice.repositories.ProjectContractorRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ConstructionProjectService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConstructionProjectServiceImpl implements ConstructionProjectService {
    private final ConstructionProjectRepository projectRepository;
    private final ProjectContractorRepository projectContractorRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @Override
    public ConstructionStatisticsDto getConstructionPageStatistics(String email) {
        log.info("Calculating syntax data for user: {}", email);
        Company company = companyService.findByUserEmail(email);

        Integer totalProjects = projectRepository.countAllByEmail(email);
        Integer activeProjects = projectRepository.countActiveByEmail(email);
        Integer completedProjects = projectRepository.countCompletedRecentByEmail(email, LocalDate.now().minusMonths(6));

        double budgetPercentage = calculateBudgetPercentage(projectRepository.getTotalBudgetByCompanyId(company.getId()), projectRepository.getTotalUsedBudgetByCompanyId(company.getId()));

        return ConstructionStatisticsDto.builder()
                .countOfProjects(totalProjects)
                .countOfActiveProjects(activeProjects)
                .countOfCompletedProjects(completedProjects)
                .budgetUsagePercentage(budgetPercentage)
                .build();
    }

    @Override
    public List<ConstructionProjectDto> getProjectsByCompany(String email) {
        log.info("Fetching all projects for user: {}", email);
        Company company = companyService.findByUserEmail(email);

        return projectRepository.findAllByCompany_Id(company.getId())
                .stream()
                .map(this::mapToBasicDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConstructionProjectDto> getFilteredProjects(ProjectStatus status, String projectName, String email) {
        log.info("Filtering projects for user: {}. status: {}, Name: {}", email, status != null ? status.name() : "ALL", projectName);
        Long companyId = companyService.findByUserEmail(email).getId();

        return projectRepository.findFilteredProjects(companyId, status, projectName)
                .stream()
                .map(this::mapToBasicDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConstructionProjectReadDto createConstructionProject(String email, ConstructionProjectCreateDto dto) {
        log.info("Creating new construction project: {} by user: {}", dto.getName(), email);
        validateBudget(dto.getBudget());

        Company company = companyService.findByUserEmail(email);

        ConstructionProject project = modelMapper.map(dto, ConstructionProject.class);
        project.setUsedBudget(BigDecimal.ZERO);
        project.setCompany(company);
        project.setStatus(dto.getStatus());
        project.setProjectContractor(projectContractorRepository.findById(dto.getProjectContractorId())
                .orElseThrow(() -> new EntityNotFoundException("Contractor not found")));

        project.setExpenses(new ArrayList<>());
        project.setStages(new ArrayList<>());
        project.setMaterials(new ArrayList<>());

        ConstructionProject savedProject = projectRepository.save(project);
        log.info("Project created successfully with ID: {}", savedProject.getId());

        return mapToDetailedDto(savedProject);
    }

    @Override
    @Transactional
    public ConstructionProjectReadDto updateConstructionProject(Long id, ConstructionProjectUpdateDto dto, String email) {
        log.info("Updating construction project ID: {} by user: {}", id, email);
        validateBudget(dto.getBudget());

        ConstructionProject entity = findProjectAndValidateOwnership(id, email);

        entity.setName(dto.getName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setBudget(dto.getBudget());
        entity.setProjectManager(dto.getProjectManager());
        entity.setStatus(dto.getStatus());

        ConstructionProject updatedProject = projectRepository.save(entity);
        return mapToDetailedDto(updatedProject);
    }

    @Override
    @Transactional
    public void deleteConstructionProject(Long id, String email) {
        log.info("Deleting construction project ID: {} by user: {}", id, email);
        ConstructionProject project = findProjectAndValidateOwnership(id, email);
        projectRepository.delete(project);
    }

    @Override
    public ConstructionProject findById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));
    }

    public List<ProjectStatusDto> getAllStatuses() {
        log.info("Fetching all available project statuses from Enum");
        return Arrays.stream(ProjectStatus.values())
                .map(status -> new ProjectStatusDto(
                        status.name(),
                        capitalizeFirstLetter(status.name())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateProjectStatus(Long id, ProjectStatus status, String email) {
        log.info("Updating status to {} for project ID: {} by user: {}", status, id, email);

        ConstructionProject project = findProjectAndValidateOwnership(id, email);

        project.setStatus(status);

        projectRepository.save(project);
    }

    @Override
    public void updateUsedBudget(BigDecimal amount, Long projectId) {
        ConstructionProject project = findById(projectId);
        project.setUsedBudget(project.getUsedBudget().add(amount));
        projectRepository.save(project);
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace("_", " ");
    }

    private void validateBudget(BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid budget provided: {}", budget);
            throw new IllegalArgumentException("Budget must be greater than or equal to zero");
        }
    }

    private ConstructionProject findProjectAndValidateOwnership(Long projectId, String email) {
        ConstructionProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + projectId));

        Long userCompanyId = companyService.findByUserEmail(email).getId();
        if (!project.getCompany().getId().equals(userCompanyId)) {
            log.warn("Security Alert: User {} tried to access project ID {}", email, projectId);
            throw new AccessDeniedException("You do not have permission to access this project");
        }
        return project;
    }

    private double calculateBudgetPercentage(BigDecimal budget, BigDecimal usedBudget) {
        if (budget != null && usedBudget != null && budget.compareTo(BigDecimal.ZERO) > 0) {
            return usedBudget.divide(budget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }
        return 0.0;
    }

    private ConstructionProjectDto mapToBasicDto(ConstructionProject project) {
        ConstructionProjectDto dto = modelMapper.map(project, ConstructionProjectDto.class);
        dto.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        return dto;
    }

    private ConstructionProjectReadDto mapToDetailedDto(ConstructionProject project) {
        ConstructionProjectReadDto dto = modelMapper.map(project, ConstructionProjectReadDto.class);
        dto.setStatus(project.getStatus() != null ? project.getStatus().name() : null);
        dto.setProjectContractorName(project.getProjectContractor() != null ? project.getProjectContractor().getName() : null);

        dto.setExpensesList(project.getExpenses().stream()
                .map(exp -> modelMapper.map(exp, ProjectExpenseReadDto.class))
                .collect(Collectors.toList()));

        dto.setStagesList(project.getStages().stream()
                .map(stage -> {
                    ProjectStageReadDto stageDto = modelMapper.map(stage, ProjectStageReadDto.class);
                    stageDto.setStagesStatus(stage.getStatus().name());
                    return stageDto;
                })
                .collect(Collectors.toList()));

        dto.setMaterialsList(project.getMaterials().stream()
                .map(material -> {
                    ConstructionProjectMaterialReadDto materialDto = new ConstructionProjectMaterialReadDto();
                    materialDto.setId(material.getId());
                    materialDto.setProjectId(project.getId());
                    materialDto.setProjectName(project.getName());
                    materialDto.setProductId(material.getProduct().getId());
                    materialDto.setProductCode(material.getProduct().getProductCode());
                    materialDto.setProductName(material.getProduct().getName());
                    materialDto.setWarehouseId(material.getWarehouse().getId());
                    materialDto.setWarehouseName(material.getWarehouse().getName());
                    materialDto.setRequiredQuantity(material.getRequiredQuantity());
                    materialDto.setUsedQuantity(material.getUsedQuantity());
                    materialDto.setAvailableQuantity(material.getWarehouseProduct().getQuantity());
                    materialDto.setLastUsedDate(material.getLastUsedDate());
                    materialDto.setNote(material.getNote());
                    if (material.getInvoice() != null) {
                        materialDto.setInvoiceId(material.getInvoice().getId());
                        materialDto.setInvoiceNumber(material.getInvoice().getInvoiceNumber());
                    }
                    return materialDto;
                })
                .collect(Collectors.toList()));
        return dto;
    }
}
