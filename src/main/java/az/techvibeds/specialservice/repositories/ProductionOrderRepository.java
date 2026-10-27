package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ProductionOrderStatus;
import az.techvibeds.specialservice.models.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;


public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {
    long countByCompanyId(Long companyId);

    Integer countByCompanyIdAndStatus(Long companyId, ProductionOrderStatus status);

    Integer countByCompanyIdAndStartDateAfter(Long companyId, Date startDate);

    @Query("SELECT COALESCE(SUM(poi.quantity), 0) FROM ProductionOrder p " +
            "JOIN p.items poi " +
            "WHERE p.company.id = :companyId AND p.startDate BETWEEN :start AND :end")
    Integer getTotalQuantityBetweenDates(@Param("companyId") Long companyId,
                                         @Param("start") Date start,
                                         @Param("end") Date end);

    @Query("SELECT DISTINCT po FROM ProductionOrder po " +
            "LEFT JOIN FETCH po.items poi " +
            "LEFT JOIN FETCH poi.manufacturedProduct mp " +
            "WHERE po.company.id = :companyId " +
            "AND (:status IS NULL OR po.status = :status) " +
            "AND (LOWER(mp.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR CAST(po.id AS string) LIKE CONCAT('%', :keyword, '%'))")
    List<ProductionOrder> findFilteredOrdersWithKeyword(
            @Param("companyId") Long companyId,
            @Param("status") ProductionOrderStatus status,
            @Param("keyword") String keyword);

    @Query("SELECT DISTINCT po FROM ProductionOrder po " +
            "LEFT JOIN FETCH po.items poi " +
            "LEFT JOIN FETCH poi.manufacturedProduct mp " +
            "WHERE po.company.id = :companyId " +
            "AND (:status IS NULL OR po.status = :status)")
    List<ProductionOrder> findFilteredOrdersWithoutKeyword(
            @Param("companyId") Long companyId,
            @Param("status") ProductionOrderStatus status);

    /** Hesabat üçün: şirkətin istehsal sifarişlərini tarix aralığı ilə gətir */
    @Query("SELECT DISTINCT po FROM ProductionOrder po " +
           "LEFT JOIN FETCH po.items poi " +
           "LEFT JOIN FETCH poi.manufacturedProduct mp " +
           "WHERE po.company.id = :companyId " +
           "AND po.startDate BETWEEN :startDate AND :endDate " +
           "ORDER BY po.startDate DESC")
    List<ProductionOrder> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
}
