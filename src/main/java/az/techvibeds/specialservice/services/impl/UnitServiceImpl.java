package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.unit.UnitCreateDto;
import az.techvibeds.specialservice.dtos.unit.UnitResponseDto;
import az.techvibeds.specialservice.dtos.unit.UnitUpdateDto;
import az.techvibeds.specialservice.enums.UnitCategory;
import az.techvibeds.specialservice.models.Unit;
import az.techvibeds.specialservice.repositories.UnitRepository;
import az.techvibeds.specialservice.services.UnitService;
import jakarta.persistence.EntityNotFoundException;
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
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;
    private final ModelMapper modelMapper;

    @Override
    public Unit findUnitById(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> {
                    log.error("Unit not found with id: {}", unitId);
                    return new EntityNotFoundException("Unit not found");
                });
    }

    @Override
    public List<UnitResponseDto> getAllUnits() {
        log.info("Fetching all units from database");
        List<Unit> units = unitRepository.findAll();

        if (units.isEmpty()) {
            log.warn("No units found in database");
        }

        return units.stream()
                .map(u -> modelMapper.map(u, UnitResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnitResponseDto> getUnitsByCategory(UnitCategory category) {
        log.info("Fetching units from database for category: {}", category);
        List<Unit> units = unitRepository.findAllByCategoryOrBoth(category);

        return units.stream()
                .map(u -> modelMapper.map(u, UnitResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UnitResponseDto create(UnitCreateDto dto) {
        log.info("Creating new unit with name: {}", dto.getName());
        Unit unit = new Unit();
        unit.setName(dto.getName());
        if (dto.getCategory() != null) {
            try {
                unit.setCategory(UnitCategory.valueOf(dto.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                unit.setCategory(UnitCategory.BOTH);
            }
        } else {
            unit.setCategory(UnitCategory.BOTH);
        }

        unitRepository.save(unit);
        log.info("Unit created successfully with ID: {}", unit.getId());

        return modelMapper.map(unit, UnitResponseDto.class);
    }

    @Override
    @Transactional
    public UnitResponseDto update(UnitUpdateDto dto) {
        log.info("Updating unit ID: {}", dto.getId());
        Unit unit = findUnitById(dto.getId());

        unit.setName(dto.getName());
        if (dto.getCategory() != null) {
            try {
                unit.setCategory(UnitCategory.valueOf(dto.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // keep existing or set BOTH
            }
        }

        unitRepository.save(unit);

        log.info("Unit ID: {} updated successfully", unit.getId());
        return modelMapper.map(unit, UnitResponseDto.class);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Attempting to delete unit ID: {}", id);
        if (!unitRepository.existsById(id)) {
            log.error("Delete failed: Unit ID {} not found", id);
            throw new EntityNotFoundException("Unit not found");
        }
        unitRepository.deleteById(id);
        log.info("Unit ID: {} deleted successfully", id);
    }

    @Override
    public UnitResponseDto getById(Long id) {
        log.info("Fetching unit details for ID: {}", id);
        Unit unit = findUnitById(id);
        return modelMapper.map(unit, UnitResponseDto.class);
    }
}
