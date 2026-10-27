package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.role.RoleRequestDto;
import az.techvibeds.specialservice.dtos.role.RoleResponseDto;
import az.techvibeds.specialservice.exceptions.ResourceNotFoundException;
import az.techvibeds.specialservice.models.Role;
import az.techvibeds.specialservice.repositories.RoleRepository;
import az.techvibeds.specialservice.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public RoleResponseDto create(RoleRequestDto dto) {
        log.info("Creating new role with name: {}", dto.getName());

        if (roleRepository.findByName(dto.getName().toUpperCase()).isPresent()) {
            log.error("Role creation failed: Role with name {} already exists", dto.getName());
            throw new RuntimeException("Role with this name already exists!");
        }

        Role role = modelMapper.map(dto, Role.class);
        role.setName(dto.getName().toUpperCase());

        Role saved = roleRepository.save(role);
        log.info("Role created successfully with ID: {}", saved.getId());

        return modelMapper.map(saved, RoleResponseDto.class);
    }

    @Override
    @Transactional
    public RoleResponseDto update(Long id, RoleRequestDto dto) {
        log.info("Updating role with ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Update failed: Role not found with ID: {}", id);
                    return new ResourceNotFoundException("Role", "id", id);
                });

        role.setName(dto.getName().toUpperCase());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());

        Role updated = roleRepository.save(role);
        log.info("Role with ID: {} updated successfully", id);

        return modelMapper.map(updated, RoleResponseDto.class);
    }

    @Override
    public RoleResponseDto getById(Long id) {
        log.debug("Fetching role with ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Fetch failed: Role not found with ID: {}", id);
                    return new ResourceNotFoundException("Role", "id", id);
                });

        return modelMapper.map(role, RoleResponseDto.class);
    }

    @Override
    public List<RoleResponseDto> getAll() {
        log.debug("Fetching all roles from database");

        List<Role> roles = roleRepository.findAll();
        log.info("Retrieved {} roles", roles.size());

        return roles.stream()
                .map(role -> modelMapper.map(role, RoleResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Attempting to delete role with ID: {}", id);

        if (!roleRepository.existsById(id)) {
            log.error("Delete failed: Role not found with ID: {}", id);
            throw new ResourceNotFoundException("Role", "id", id);
        }

        roleRepository.deleteById(id);
        log.info("Role with ID: {} deleted successfully", id);
    }
}