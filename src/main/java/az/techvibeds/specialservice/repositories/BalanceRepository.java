package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRepository extends JpaRepository<Balance, Long> {
}
