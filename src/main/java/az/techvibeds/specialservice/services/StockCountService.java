package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.inventory.StockCountCreateDto;
import az.techvibeds.specialservice.dtos.inventory.StockCountReadDto;

import java.util.List;

public interface StockCountService {
    StockCountReadDto createStockCount(StockCountCreateDto dto, String userEmail);
    StockCountReadDto updateStockCount(Long id, StockCountCreateDto dto, String userEmail);
    StockCountReadDto getStockCountById(Long id, String userEmail);
    List<StockCountReadDto> getAllStockCounts(String userEmail);
    StockCountReadDto completeStockCount(Long id, String userEmail);
    void deleteStockCount(Long id, String userEmail);
}
