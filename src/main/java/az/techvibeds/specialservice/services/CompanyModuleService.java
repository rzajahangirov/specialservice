package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.module.ModuleReadDto;

import java.util.List;

public interface CompanyModuleService {
    List<ModuleReadDto> getCompanysModules(String email);
}
