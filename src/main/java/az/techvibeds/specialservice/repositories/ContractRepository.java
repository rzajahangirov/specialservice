package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByEmployeeIdAndIsActiveTrue(Long employeeId);


}
