package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorReadDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorUpdateDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorCreateDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorDto;

import java.util.List;

public interface ProjectContractorService {

    ProjectContractorReadDto create(ProjectContractorCreateDto dto, String email);

    ProjectContractorReadDto getById(Long id, String userEmail);

    ProjectContractorReadDto update(Long id, ProjectContractorUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    List<ProjectContractorDto> getFilteredContractors(String contractorName, String userEmail);
}
