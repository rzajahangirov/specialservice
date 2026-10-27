package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ProjectStatus;
import az.techvibeds.specialservice.models.ConstructionProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConstructionProjectRepository extends JpaRepository<ConstructionProject, Long> {
    List<ConstructionProject> findAllByCompany_Id(Long companyId);


    @Query("SELECT COUNT(p) FROM ConstructionProject p JOIN p.company c JOIN c.users u WHERE u.email = :email")
    Integer countAllByEmail(@Param("email") String email);


    @Query("SELECT COUNT(p) FROM ConstructionProject p JOIN p.company c JOIN c.users u " +
            "WHERE u.email = :email AND p.status = az.techvibeds.specialservice.enums.ProjectStatus.ACTIVE")
    Integer countActiveByEmail(@Param("email") String email);


    @Query("SELECT COUNT(p) FROM ConstructionProject p JOIN p.company c JOIN c.users u " +
            "WHERE u.email = :email AND p.status = az.techvibeds.specialservice.enums.ProjectStatus.COMPLETED " +
            "AND p.endDate > :sixMonthsAgo")
    Integer countCompletedRecentByEmail(@Param("email") String email, @Param("sixMonthsAgo") LocalDate sixMonthsAgo);


    @Query("SELECT p FROM ConstructionProject p WHERE p.company.id = :companyId " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:projectName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :projectName, '%')))")
    List<ConstructionProject> findFilteredProjects(
            @Param("companyId") Long companyId,
            @Param("status") ProjectStatus status,
            @Param("projectName") String projectName);

    @Query("SELECT SUM(p.budget) FROM ConstructionProject p WHERE p.company.id = :companyId")
    BigDecimal getTotalBudgetByCompanyId(@Param("companyId") Long companyId);


    @Query("SELECT SUM(p.usedBudget) FROM ConstructionProject p WHERE p.company.id = :companyId")
    BigDecimal getTotalUsedBudgetByCompanyId(@Param("companyId") Long companyId);
}

