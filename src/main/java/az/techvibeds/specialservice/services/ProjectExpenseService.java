package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseCreateDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseReadDto;
import az.techvibeds.specialservice.dtos.projectExpense.ProjectExpenseUpdateDto;

import java.util.List;

public interface ProjectExpenseService {
    ProjectExpenseReadDto create(ProjectExpenseCreateDto dto, String email, Long projectId);

    ProjectExpenseReadDto getById(Long id, String userEmail);

    List<ProjectExpenseDto> getAllByCompany(String userEmail);

    ProjectExpenseReadDto update(Long id, ProjectExpenseUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);
}
