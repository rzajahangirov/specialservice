package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.dtos.companyStock.CompanyStockInventoryDto;

public interface CompanyStockService {
    CompanyStockInventoryDto findStockCount(Long id);
}
