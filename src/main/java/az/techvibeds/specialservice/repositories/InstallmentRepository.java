package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findAllByPaymentPlanId(Long paymentPlanId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Installment i " +
            "WHERE i.paymentPlan.company.id = :companyId " +
            "AND i.paid = false " +
            "AND i.dueDate BETWEEN :startDate AND :endDate")
    BigDecimal sumUpcomingUnpaidByCompanyAndDueDate(@Param("companyId") Long companyId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}
