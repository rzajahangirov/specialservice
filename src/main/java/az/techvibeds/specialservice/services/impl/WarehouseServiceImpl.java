package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.warehouse.WarehouseCreateDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseReadDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseUpdateDto;
import az.techvibeds.specialservice.exceptions.BusinessException;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Warehouse;
import az.techvibeds.specialservice.repositories.WarehouseRepository;
import az.techvibeds.specialservice.services.CompanyService;
import az.techvibeds.specialservice.services.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final CompanyService companyService;

    @Override
    @Transactional(readOnly = true)
    public Warehouse getWarehouseByName(String warehouseName) {
        Warehouse warehouse = warehouseRepository.findByName(warehouseName);
        if (warehouse == null) {
            throw new BusinessException("Warehouse not found with name: " + warehouseName, HttpStatus.NOT_FOUND);
        }
        return warehouse;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllByCompany_Id(Long companyId) {
        return warehouseRepository.findAllByCompany_Id(companyId);
    }

    @Override
    @Transactional
    public WarehouseReadDto create(WarehouseCreateDto dto, String email) {
        Company company = companyService.findByUserEmail(email);

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress());
        warehouse.setCompany(company);
        warehouse.setActive(true);

        warehouseRepository.save(warehouse);
        log.info("Warehouse created: {} for company: {}", warehouse.getName(), company.getName());
        return mapToReadDto(warehouse);
    }

    @Override
    @Transactional
    public WarehouseReadDto update(WarehouseUpdateDto dto, String userEmail) {
        Warehouse warehouse = findById(dto.getId());
        validateOwnership(warehouse, userEmail);

        warehouse.setName(dto.getName());
        warehouse.setAddress(dto.getAddress());

        warehouseRepository.save(warehouse);
        log.info("Warehouse updated: id={}", warehouse.getId());
        return mapToReadDto(warehouse);
    }

    @Override
    @Transactional
    public void delete(Long id, String userEmail) {
        Warehouse warehouse = findById(id);
        validateOwnership(warehouse, userEmail);

        warehouseRepository.delete(warehouse);
        log.warn("Warehouse deleted: id={} by user={}", id, userEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseReadDto getById(Long id, String userEmail) {
        Warehouse warehouse = findById(id);
        validateOwnership(warehouse, userEmail);
        return mapToReadDto(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseReadDto> getAllByCompanyReadDto(String email) {
        Company company = companyService.findByUserEmail(email);
        if (company == null) {
            // Super admin sees all warehouses
            return warehouseRepository.findAll()
                    .stream()
                    .map(this::mapToReadDto)
                    .toList();
        }
        return warehouseRepository.findAllByCompany_Id(company.getId())
                .stream()
                .map(this::mapToReadDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(Long warehouseId) {
        return findById(warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Warehouse findById(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BusinessException("Warehouse not found with id: " + warehouseId, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountOfActiveWarehouses(Long companyId) {
        return warehouseRepository.countByCompanyIdAndActiveTrue(companyId);
    }

    @Override
    @Transactional
    public void toggleWarehouseStatus(Long warehouseId, String userEmail) {
        Warehouse warehouse = findById(warehouseId);
        validateOwnership(warehouse, userEmail);

        warehouse.setActive(!warehouse.getActive());
        warehouseRepository.save(warehouse);
        log.info("Warehouse status toggled: id={}, active={}", warehouseId, warehouse.getActive());
    }

    private void validateOwnership(Warehouse warehouse, String userEmail) {
        Company userCompany = companyService.findByUserEmail(userEmail);
        
        if (userCompany == null) {
            // Super Admin check
            return;
        }

        if (warehouse.getCompany() != null && !warehouse.getCompany().getId().equals(userCompany.getId())) {
            throw new BusinessException("You do not have permission to access this warehouse", HttpStatus.FORBIDDEN);
        }
    }

    private WarehouseReadDto mapToReadDto(Warehouse warehouse) {
        return WarehouseReadDto.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .address(warehouse.getAddress())
                .active(warehouse.getActive())
                .companyName(warehouse.getCompany() != null ? warehouse.getCompany().getName() : null)
                .build();
    }
}