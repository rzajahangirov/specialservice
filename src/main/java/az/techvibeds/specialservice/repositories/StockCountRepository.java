package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    List<StockCount> findAllByCompanyId(Long companyId);
    Optional<StockCount> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCountNumber(String countNumber);
}
