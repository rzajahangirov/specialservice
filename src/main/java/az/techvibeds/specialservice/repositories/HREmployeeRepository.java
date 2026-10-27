package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.EmploymentStatus;
import az.techvibeds.specialservice.models.HREmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HREmployeeRepository extends JpaRepository<HREmployee, Long> {
    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM hr_employees WHERE id = :id", nativeQuery = true)
    Optional<HREmployee> findByIdWithDeleted(@Param("id") Long id);

    @Query("SELECT e FROM HREmployee e WHERE e.company.id = :companyId AND (:status IS NULL OR e.status = :status)")
    List<HREmployee> findAllByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") EmploymentStatus status
    );

    @Query(value = "SELECT * FROM hr_employees WHERE company_id = :companyId AND deleted = true",
            nativeQuery = true)
    List<HREmployee> findDeletedEmployeesByCompanyId(@Param("companyId") Long companyId);

    @Query(value = "SELECT * FROM hr_employees WHERE id = :id AND deleted = true",
            nativeQuery = true)
    Optional<HREmployee> findDeletedById(@Param("id") Long id);

    Optional<HREmployee> findByIdAndDeletedFalse(Long id);

    @Query("SELECT e FROM HREmployee e WHERE e.email = :email AND e.deleted = false")
    Optional<HREmployee> findActiveByEmail(@Param("email") String email);

    long countByCompanyIdAndDeletedFalse(Long companyId);

    List<HREmployee> findAllByCompanyIdAndDeletedFalse(Long companyId);

    long countByCompanyIdAndDeletedFalseAndStatus(Long companyId, EmploymentStatus employmentStatus);
}
