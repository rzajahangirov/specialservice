package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {

    @Query(value = """
            SELECT * FROM deductions
            WHERE employee_id = :employeeId
              AND is_active    = true
              AND start_date  <= :periodStart
              AND (start_date + CAST((duration || ' ' || duration_type) AS INTERVAL)) > :periodStart
            """, nativeQuery = true)
    List<Deduction> findActiveDeductionsForPeriod(
            @Param("employeeId")  Long      employeeId,
            @Param("periodStart") LocalDate periodStart
    );

    @Query(value = """
            SELECT * FROM deductions
            WHERE is_active = true
              AND (start_date + CAST((duration || ' ' || duration_type) AS INTERVAL)) <= :today
            """, nativeQuery = true)
    List<Deduction> findExpiredDeductions(@Param("today") LocalDate today);

    Optional<Deduction> findByIdAndDeletedFalse(Long id);

    List<Deduction> findAllByEmployeeCompanyIdAndDeletedTrue(Long companyId);

    Optional<Deduction> findByIdAndDeletedTrue(Long id);

    List<Deduction> findAllByEmployeeIdAndDeletedFalseOrderByStartDateDesc(Long employeeId);
}
