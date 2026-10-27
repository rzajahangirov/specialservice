package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    Company findByUsers_Id(Long userId);
}
