package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.inventory.InventoryStatisticsDto;
import az.techvibeds.specialservice.dtos.inventory.ProductStatusDto;

public interface InventoryService {
    InventoryStatisticsDto getInventory(String name);

    ProductStatusDto getProductStatuses();
}
