package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.CurrencyRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRateHistoryRepository extends JpaRepository<CurrencyRateHistory, Long> {
    List<CurrencyRateHistory> findAllByCurrencyCodeOrderByRateDateDesc(String currencyCode);
}
