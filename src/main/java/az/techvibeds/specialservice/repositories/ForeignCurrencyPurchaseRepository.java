package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ForeignCurrencyPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForeignCurrencyPurchaseRepository extends JpaRepository<ForeignCurrencyPurchase, Long> {
    Page<ForeignCurrencyPurchase> findAllByCompanyId(Long companyId, Pageable pageable);
    List<ForeignCurrencyPurchase> findAllByCompanyId(Long companyId);
}
