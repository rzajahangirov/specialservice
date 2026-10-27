package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByCompany_Id(Long companyId);

    Product findByProductCode(String productCode);

    @Query("SELECT p FROM Product p WHERE p.company.id = :companyId " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProductsByCompanyAndKeyword(@Param("companyId") Long companyId,
                                                    @Param("keyword") String keyword);

    @Query("SELECT SUM(wp.quantity) FROM Product p JOIN p.warehouseProducts wp WHERE p.company.id = :companyId")
    Optional<Long> sumStockByCompanyId(Long companyId);

    Long countByCompanyId(Long companyId);

    Optional<Product> findByProductCodeAndCompanyId(String code, Long companyId);

    Optional<Product> findFirstByProductCodeAndCompanyIdOrderByIdDesc(String code, Long companyId);

    Optional<Product> findFirstByProductCodeAndCompanyIdOrderByIdAsc(String code, Long companyId);
}
