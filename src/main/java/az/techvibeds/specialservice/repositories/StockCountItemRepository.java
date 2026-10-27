package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.StockCountItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCountItemRepository extends JpaRepository<StockCountItem, Long> {
    List<StockCountItem> findAllByStockCountId(Long stockCountId);
}
