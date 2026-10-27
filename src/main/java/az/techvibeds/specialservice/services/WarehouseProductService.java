package az.techvibeds.specialservice.services;

import az.techvibeds.specialservice.models.Product;
import az.techvibeds.specialservice.models.SemiProduct;
import az.techvibeds.specialservice.models.Warehouse;
import az.techvibeds.specialservice.models.WarehouseProduct;

import java.util.List;

public interface WarehouseProductService {
    WarehouseProduct createWarehouseProduct(Product product, Long warehouseId, double quantity);
    WarehouseProduct createWarehouseSemiProduct(SemiProduct semiProduct, Long warehouseId, double quantity, String userEmail);

    void removeFromWarehouse(String productCode, Long warehouseId, Integer quantity, String userEmail) throws Exception;

    void transferProductBetweenWarehouses(String productCode, Long fromWarehouseId, Long toWarehouseId, int quantity, String userEmail) throws Exception;

    Integer updateInventoryWarehouseProduct(WarehouseProduct warehouseProduct, Long warehouseId, Integer quantity);

    WarehouseProduct findWarehouseProductById(Long id);

    WarehouseProduct saveWarehouseProduct(WarehouseProduct warehouseProduct);

    void deleteWarehouseProduct(Long id, String userEmail);

    WarehouseProduct createWarehouseProductFromExcel(Product product, String warehouseName, double quantity);

    List<WarehouseProduct> findWarehouseProductByCompany_Id(List<Warehouse> warehouseList);

    List<WarehouseProduct> findWarehouseProductByWarehouseId(Long warehouseId);

    List<WarehouseProduct> findFiltered(Long companyId, Long warehouseId);

    WarehouseProduct findByProductAndWarehouse(Long productId, Long warehouseId);
}
