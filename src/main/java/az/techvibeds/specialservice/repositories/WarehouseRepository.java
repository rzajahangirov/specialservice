package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Warehouse;
import az.techvibeds.specialservice.models.WarehouseProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Warehouse findByName(String name);
    Optional<Warehouse> findByNameAndCompanyId(String name, Long companyId);

    List<Warehouse> findAllByCompany_Id(Long companyId);

    Long countByCompanyId(Long companyId);

    Long countByCompanyIdAndActiveTrue(Long companyId);

    @Query("SELECT wp FROM WarehouseProduct wp JOIN FETCH wp.product JOIN FETCH wp.warehouse WHERE wp.warehouse.company.id = :companyId")
    List<WarehouseProduct> findAllByWarehouse_Company_Id(@Param("companyId") Long companyId);

    @Query("SELECT wp FROM WarehouseProduct wp JOIN FETCH wp.product WHERE wp.warehouse.id = :warehouseId")
    List<WarehouseProduct> findAllByWarehouse_Id(@Param("warehouseId") Long warehouseId);
}
