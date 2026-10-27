package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ConstructionProjectMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConstructionProjectMaterialRepository extends JpaRepository<ConstructionProjectMaterial, Long> {
    List<ConstructionProjectMaterial> findAllByProject_Id(Long projectId);

    Optional<ConstructionProjectMaterial> findByProject_IdAndProduct_ProductCodeAndWarehouse_Id(
            Long projectId, String productCode, Long warehouseId);
}
