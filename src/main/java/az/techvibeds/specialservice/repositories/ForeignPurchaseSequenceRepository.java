package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ForeignPurchaseSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForeignPurchaseSequenceRepository extends JpaRepository<ForeignPurchaseSequence, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ForeignPurchaseSequence> findByCompanyIdAndYear(Long companyId, Integer year);
}
