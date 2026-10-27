package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ForeignPurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForeignPurchaseItemRepository extends JpaRepository<ForeignPurchaseItem, Long> {
}
