package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Product;
import az.techvibeds.specialservice.models.SemiProduct;
import az.techvibeds.specialservice.models.Warehouse;
import az.techvibeds.specialservice.models.WarehouseProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseProductRepository extends JpaRepository<WarehouseProduct, Long> {
    WarehouseProduct findByProductAndWarehouse(Product product, Warehouse warehouse);
    WarehouseProduct findBySemiProductAndWarehouse(SemiProduct semiProduct, Warehouse warehouse);

    Optional<WarehouseProduct> findByProduct_ProductCodeAndWarehouse_IdAndProduct_Company_Id(
            String productCode, Long warehouseId, Long companyId);

    List<WarehouseProduct> findAllByWarehouse_Id(Long warehouseId);

    List<WarehouseProduct> findAllByWarehouse_Company_Id(Long companyId);
    List<WarehouseProduct> findAllByProduct(Product product);

    Optional<WarehouseProduct> findByProduct_IdAndWarehouse_Id(Long productId, Long warehouseId);

    Optional<WarehouseProduct> findFirstByProduct_IdAndWarehouse_Company_IdAndQuantityGreaterThanOrderByQuantityDesc(
            Long productId, Long companyId, Integer quantity);
}
