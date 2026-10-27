package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.role.RoleRequestDto;
import az.techvibeds.specialservice.dtos.role.RoleResponseDto;

import java.util.List;

public interface RoleService {
    RoleResponseDto create(RoleRequestDto dto);
    RoleResponseDto update(Long id, RoleRequestDto dto);
    RoleResponseDto getById(Long id);
    List<RoleResponseDto> getAll();
    void delete(Long id);
}
