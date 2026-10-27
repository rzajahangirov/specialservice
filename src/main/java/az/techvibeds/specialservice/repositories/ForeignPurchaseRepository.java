package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ForeignPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForeignPurchaseRepository extends JpaRepository<ForeignPurchase, Long> {
    Page<ForeignPurchase> findAllByCompanyId(Long companyId, Pageable pageable);
    List<ForeignPurchase> findAllByCompanyId(Long companyId);
}
