package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseReadDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseUpdateDto;
import az.techvibeds.specialservice.models.ConstructionProject;
import az.techvibeds.specialservice.models.ProjectExpense;
import az.techvibeds.specialservice.repositories.ProjectExpenseRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ConstructionProjectService;
import az.techvibeds.specialservice.services.ProjectExpenseService;
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
public class ProjectExpenseServiceImpl implements ProjectExpenseService {
    private final ProjectExpenseRepository expenseRepository;
    private final ConstructionProjectService projectService;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ProjectExpenseReadDto create(ProjectExpenseCreateDto dto, String email, Long projectId) {
        log.info("Adding expense to project ID: {} by user: {}", projectId, email);

        ConstructionProject project = projectService.findById(projectId);

        Long userCompanyId = companyService.findByUserEmail(email).getId();
        if (!project.getCompany().getId().equals(userCompanyId)) {
            throw new AccessDeniedException("You cannot add expense to a project that doesn't belong to your company");
        }

        ProjectExpense expense = modelMapper.map(dto, ProjectExpense.class);
        expense.setProject(project);

        projectService.updateUsedBudget(dto.getAmount(), projectId);

        ProjectExpense saved = expenseRepository.save(expense);
        return mapToReadDto(saved);
    }

    @Override
    public ProjectExpenseReadDto getById(Long id, String email) {
        ProjectExpense expense = findAndValidateOwner(id, email);
        return mapToReadDto(expense);
    }

    @Override
    public List<ProjectExpenseDto> getAllByCompany(String email) {
        log.info("Fetching all expenses for company of user: {}", email);
        Long companyId = companyService.findByUserEmail(email).getId();

        return expenseRepository.findAllByCompanyIdWithProject(companyId)
                .stream()
                .map(e -> {
                    ProjectExpenseDto dto = modelMapper.map(e, ProjectExpenseDto.class);
                    dto.setProjectName(e.getProject().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectExpenseReadDto update(Long id, ProjectExpenseUpdateDto dto, String email) {
        ProjectExpense expense = findAndValidateOwner(id, email);

        expense.setName(dto.getName());
        expense.setAmount(dto.getAmount());
        expense.setDate(dto.getDate());

        log.info("Expense ID: {} updated successfully", id);
        return mapToReadDto(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        ProjectExpense expense = findAndValidateOwner(id, email);
        expenseRepository.delete(expense);
        log.info("Expense ID: {} deleted", id);
    }

    private ProjectExpense findAndValidateOwner(Long id, String email) {
        ProjectExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found"));

        Long companyId = companyService.findByUserEmail(email).getId();
        if (!expense.getProject().getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("Access denied to this expense");
        }
        return expense;
    }

    private ProjectExpenseReadDto mapToReadDto(ProjectExpense expense) {
        ProjectExpenseReadDto dto = modelMapper.map(expense, ProjectExpenseReadDto.class);
        dto.setProjectName(expense.getProject().getName());
        return dto;
    }
}
