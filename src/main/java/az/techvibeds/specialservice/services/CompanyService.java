package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.company.CompanyCreateDto;
import az.techvibeds.specialservice.dtos.company.CompanyReadDto;
import az.techvibeds.specialservice.dtos.company.CompanyUpdateDto;
import az.techvibeds.specialservice.models.Company;

import java.util.List;

public interface CompanyService {
    Company findByUserEmail(String name);
    CompanyReadDto create(CompanyCreateDto createDto);
    CompanyReadDto update(CompanyUpdateDto updateDto);
    void delete(Long id);
    CompanyReadDto getById(Long id);
    List<CompanyReadDto> getAll(String email);

    List<Company> findAll();
}
