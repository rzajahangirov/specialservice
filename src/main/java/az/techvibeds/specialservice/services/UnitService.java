package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.unit.UnitCreateDto;
import az.techvibeds.specialservice.dtos.unit.UnitResponseDto;
import az.techvibeds.specialservice.dtos.unit.UnitUpdateDto;
import az.techvibeds.specialservice.enums.UnitCategory;
import az.techvibeds.specialservice.models.Unit;

import java.util.List;

public interface UnitService {
    Unit findUnitById(Long unitId);

    List<UnitResponseDto> getAllUnits();

    List<UnitResponseDto> getUnitsByCategory(UnitCategory category);

    UnitResponseDto create(UnitCreateDto dto);

    UnitResponseDto update(UnitUpdateDto dto);

    void delete(Long id);

    UnitResponseDto getById(Long id);
}
