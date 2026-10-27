package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.warehouse.WarehouseCreateDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseReadDto;
import az.techvibeds.specialservice.dtos.warehouse.WarehouseUpdateDto;
import az.techvibeds.specialservice.models.Warehouse;

import java.util.List;

public interface WarehouseService {

    Warehouse getWarehouseByName(String warehouseName);

    List<Warehouse> findAllByCompany_Id(Long companyId);

    WarehouseReadDto create(WarehouseCreateDto dto, String email);

    WarehouseReadDto update(WarehouseUpdateDto dto, String userEmail);

    void delete(Long id, String userEmail);

    WarehouseReadDto getById(Long id, String userEmail);

    List<WarehouseReadDto> getAllByCompanyReadDto(String email);

    Warehouse getWarehouseById(Long warehouseId);

    Warehouse findById(Long warehouseId);

    Long getCountOfActiveWarehouses(Long companyId);

    void toggleWarehouseStatus(Long warehouseId, String userEmail);
}
