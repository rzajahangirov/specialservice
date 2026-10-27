package az.techvibeds.specialservice.services.impl;

import az.techvibeds.specialservice.dtos.companyStock.CompanyStockInventoryDto;
import az.techvibeds.specialservice.dtos.inventory.InventoryStatisticsDto;
import az.techvibeds.specialservice.dtos.inventory.ProductStatusDto;
import az.techvibeds.specialservice.enums.ProductStatus;
import az.techvibeds.specialservice.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final CompanyService companyService;
    private final CompanyStockService companyStockService;
    private final ProductService productService;
    private final WarehouseActivityService warehouseActivityService;
    private final WarehouseService warehouseService;

    @Override
    public InventoryStatisticsDto getInventory(String userEmail) {
        Long companyId = companyService.findByUserEmail(userEmail).getId();

        CompanyStockInventoryDto stockInfo = companyStockService.findStockCount(companyId);

        return InventoryStatisticsDto.builder()
                .productCount(productService.getProductCountByCompany(companyId))
                .activeWarehouseCount(warehouseService.getCountOfActiveWarehouses(companyId))
                .lastTransfer(warehouseActivityService.getLastTransfer(companyId))
                .stockCount(productService.calculateCompanyStock(companyId))
                .growthRate(stockInfo != null ? stockInfo.getGrowthRate() + "%" : "0%")
                .build();
    }

    @Override
    public ProductStatusDto getProductStatuses() {
        ProductStatusDto productStatusDto = new ProductStatusDto();
        productStatusDto.setProductStatus(
                Arrays.stream(ProductStatus.values())
                        .map(Enum ::name)
                        .toList()
        );
        return productStatusDto;
    }
}
