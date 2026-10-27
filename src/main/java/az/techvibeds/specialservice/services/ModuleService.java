package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.module.ModuleCreateDto;
import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import az.techvibeds.specialservice.dtos.module.ModuleUpdateDto;

import java.util.List;

public interface ModuleService {
    ModuleReadDto create(ModuleCreateDto createDto);
    ModuleReadDto update(ModuleUpdateDto updateDto);
    void delete(Long id);
    ModuleReadDto getById(Long id);
    List<ModuleReadDto> getAll();
}
