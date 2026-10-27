package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ManufacturedProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManufacturedProductRepository extends JpaRepository<ManufacturedProduct, Long> {
    List<ManufacturedProduct> findAllByCompany_Id(Long companyId);
    Optional<ManufacturedProduct> findByNameAndCompany_Id(String name, Long companyId);
}
