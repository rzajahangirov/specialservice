package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorReadDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorUpdateDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorCreateDto;
import az.techvibeds.specialservice.dtos.projectContractor.ProjectContractorDto;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.ProjectContractor;
import az.techvibeds.specialservice.repositories.ProjectContractorRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.ProjectContractorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectContractorServiceImpl implements ProjectContractorService {
    private final ProjectContractorRepository contractorRepository;
    private final CompanyService companyService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ProjectContractorReadDto create(ProjectContractorCreateDto dto, String email) {
        log.info("Creating project contractor: {} for company: {}", dto.getName(), email);

        Company company = companyService.findByUserEmail(email);
        ProjectContractor contractor = modelMapper.map(dto, ProjectContractor.class);

        contractor.setCompany(company);
        contractor.setConstructionProjects(new ArrayList<>()); // Initialization

        ProjectContractor saved = contractorRepository.save(contractor);
        return modelMapper.map(saved, ProjectContractorReadDto.class);
    }

    @Override
    public ProjectContractorReadDto getById(Long id, String email) {
        log.info("Fetching contractor ID: {}", id);
        ProjectContractor contractor = findAndValidateOwner(id, email);
        return modelMapper.map(contractor, ProjectContractorReadDto.class);
    }

    @Override
    public List<ProjectContractorDto> getFilteredContractors(String name, String email) {
        log.info("Filtering contractors with name: {} for user: {}", name, email);
        Long companyId = companyService.findByUserEmail(email).getId();

        return contractorRepository.findByCompanyAndName(companyId, name)
                .stream()
                .map(c -> modelMapper.map(c, ProjectContractorDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectContractorReadDto update(Long id, ProjectContractorUpdateDto dto, String email) {
        log.info("Updating contractor ID: {}", id);
        ProjectContractor contractor = findAndValidateOwner(id, email);

        contractor.setName(dto.getName());
        contractor.setContactPerson(dto.getContactPerson());
        contractor.setPhoneNumber(dto.getPhoneNumber());
        contractor.setEmail(dto.getEmail());

        return modelMapper.map(contractorRepository.save(contractor), ProjectContractorReadDto.class);
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        log.info("Deleting contractor ID: {}", id);
        ProjectContractor contractor = findAndValidateOwner(id, email);
        contractorRepository.delete(contractor);
    }

    private ProjectContractor findAndValidateOwner(Long id, String email) {
        ProjectContractor contractor = contractorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contractor not found with ID: " + id));

        Company company = companyService.findByUserEmail(email);
        if (!contractor.getCompany().getId().equals(company.getId())) {
            log.error("Access Denied! User {} tried to access contractor ID {}", email, id);
            throw new AccessDeniedException("You do not have permission to access this contractor");
        }
        return contractor;
    }
}
