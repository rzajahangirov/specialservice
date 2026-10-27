package az.techvibeds.specialservice.repositories;


import az.techvibeds.specialservice.models.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    boolean existsByEmployeeIdAndPeriod(Long id, YearMonth period);

    List<Payroll> findAllByEmployeeIdOrderByPeriodDesc(Long employeeId);

    @Query("SELECT p FROM Payroll p WHERE p.employee.company.id = :companyId " +
            "AND (:period IS NULL OR p.period = :period) " +
            "AND (:isPaid IS NULL OR p.isPaid = :isPaid)")
    List<Payroll> findAllByFilters(Long companyId, YearMonth period, Boolean isPaid);

    @Query("SELECT SUM(p.netSalary) FROM Payroll p")
    java.math.BigDecimal sumAllNetSalaries();
}