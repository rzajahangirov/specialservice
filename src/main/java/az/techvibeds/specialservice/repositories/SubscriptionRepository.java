package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findFirstByCompanyIdOrderByEndDateDesc(Long companyId);

    List<Subscription> findByCompanyId(Long companyId);

    @Query("SELECT SUM(s.pricePaid) FROM Subscription s")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(s.pricePaid), 0) FROM Subscription s " +
            "WHERE s.company.id = :companyId " +
            "AND s.startDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByCompanyAndDateRange(@Param("companyId") Long companyId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT EXTRACT(MONTH FROM s.startDate) as month, SUM(s.pricePaid) as total FROM Subscription s GROUP BY EXTRACT(MONTH FROM s.startDate)")
    List<Object[]> getSalesTrendByMonth();

    @Query("SELECT EXTRACT(MONTH FROM s.startDate) as month, COALESCE(SUM(s.pricePaid), 0) as total " +
            "FROM Subscription s " +
            "WHERE s.company.id = :companyId " +
            "AND EXTRACT(YEAR FROM s.startDate) = :year " +
            "GROUP BY EXTRACT(MONTH FROM s.startDate)")
    List<Object[]> getSalesTrendByCompanyAndYear(@Param("companyId") Long companyId,
                                                 @Param("year") int year);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<Subscription> findActiveExpiredSubscriptions(@Param("now") LocalDate now);
}
