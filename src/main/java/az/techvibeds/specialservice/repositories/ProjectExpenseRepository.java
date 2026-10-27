package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.ProjectExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, Long> {
    @Query("SELECT pe FROM ProjectExpense pe " +
            "JOIN FETCH pe.project p " +
            "JOIN p.company c " +
            "JOIN c.users u " +
            "WHERE u.email = :email")
    List<ProjectExpense> findAllByCompanyUserEmail(@Param("email") String email);

    @Query("SELECT e FROM ProjectExpense e JOIN FETCH e.project p WHERE p.company.id = :companyId")
    List<ProjectExpense> findAllByCompanyIdWithProject(@Param("companyId") Long companyId);

    @Query("SELECT SUM(e.amount) FROM ProjectExpense e")
    java.math.BigDecimal sumAllAmounts();

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ProjectExpense e " +
            "WHERE e.project.company.id = :companyId " +
            "AND e.date BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
