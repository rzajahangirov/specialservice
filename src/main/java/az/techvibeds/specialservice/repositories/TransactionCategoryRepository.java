package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.models.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
    List<TransactionCategory> findByCompanyId(Long companyId);
    List<TransactionCategory> findByCompanyIdAndType(Long companyId, TransactionType type);
    java.util.Optional<TransactionCategory> findByCompanyIdAndTypeAndName(Long companyId, TransactionType type, String name);
}
