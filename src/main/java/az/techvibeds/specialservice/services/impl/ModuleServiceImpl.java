package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.module.ModuleCreateDto;
import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import az.techvibeds.specialservice.dtos.module.ModuleUpdateDto;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Module;
import az.techvibeds.specialservice.repositories.ModuleRepository;
import az.techvibeds.specialservice.services.ModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;

    @Override
    @Transactional
    public ModuleReadDto create(ModuleCreateDto createDto) {
        // Eyni backendKey ilə modul olmamalıdır
        if (createDto.getBackendKey() != null &&
                moduleRepository.existsByBackendKey(createDto.getBackendKey())) {
            throw new IllegalArgumentException(
                    "Bu backendKey artıq mövcuddur: " + createDto.getBackendKey());
        }

        Module module = new Module();
        module.setName(createDto.getName());
        module.setDescription(createDto.getDescription() != null ? createDto.getDescription() : "");
        module.setIcon(createDto.getIcon() != null ? createDto.getIcon() : "");
        module.setBackendKey(createDto.getBackendKey() != null
                ? createDto.getBackendKey().toUpperCase().trim() : null);

        Module saved = moduleRepository.save(module);
        log.info("Module created: id={}, backendKey={}", saved.getId(), saved.getBackendKey());
        return mapToReadDto(saved);
    }

    @Override
    @Transactional
    public ModuleReadDto update(ModuleUpdateDto updateDto) {
        Module module = moduleRepository.findById(updateDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", updateDto.getId()));

        if (updateDto.getName() != null) module.setName(updateDto.getName());
        if (updateDto.getDescription() != null) module.setDescription(updateDto.getDescription());
        if (updateDto.getIcon() != null) module.setIcon(updateDto.getIcon());
        if (updateDto.getBackendKey() != null) {
            String newKey = updateDto.getBackendKey().toUpperCase().trim();
            // Başqa bir modul eyni key-i işlətmirmi?
            moduleRepository.findByBackendKey(newKey).ifPresent(existing -> {
                if (!existing.getId().equals(module.getId())) {
                    throw new IllegalArgumentException(
                            "Bu backendKey artıq başqa modulda işlədilir: " + newKey);
                }
            });
            module.setBackendKey(newKey);
        }

        Module saved = moduleRepository.save(module);
        log.info("Module updated: id={}, backendKey={}", saved.getId(), saved.getBackendKey());
        return mapToReadDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));
        moduleRepository.delete(module);
        log.info("Module deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleReadDto getById(Long id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module", "id", id));
        return mapToReadDto(module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleReadDto> getAll() {
        return moduleRepository.findAll().stream()
                .map(this::mapToReadDto)
                .collect(Collectors.toList());
    }

    private ModuleReadDto mapToReadDto(Module module) {
        return ModuleReadDto.builder()
                .id(module.getId())
                .name(module.getName())
                .icon(module.getIcon())
                .backendKey(module.getBackendKey())
                .build();
    }
}

