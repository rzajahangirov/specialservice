package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Account a WHERE a.id = :id")
    java.util.Optional<Account> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
