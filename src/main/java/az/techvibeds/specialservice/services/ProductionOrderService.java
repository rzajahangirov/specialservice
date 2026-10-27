package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderCreateDto;
import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderReadDto;
import az.techvibeds.specialservice.dtos.productionOrder.ProductionOrderUpdateDto;
import az.techvibeds.specialservice.enums.ProductionOrderStatus;

import java.util.List;

public interface ProductionOrderService {
    ProductionOrderReadDto createProductionServiceAndProduct(ProductionOrderCreateDto createDto, String userEmail);

    ProductionOrderReadDto updateProductionOrderAndProduct(Long id, ProductionOrderUpdateDto updateDto, String userEmail);

    void deleteById(Long id, String principal);

    List<ProductionOrderReadDto> getOrders(String keyword, ProductionOrderStatus status, String name);

    ProductionOrderReadDto getOrderById(Long id, String userEmail);
}
