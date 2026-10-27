package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ActivityTypeWarehouse;
import az.techvibeds.specialservice.models.WarehouseActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseActivityRepository extends JpaRepository<WarehouseActivity, Long> {
    @Query("""
    SELECT w 
    FROM WarehouseActivity w 
    WHERE w.type = :type AND w.company.id = :companyId 
    ORDER BY w.date DESC
""")
    WarehouseActivity findTopByTypeAndCompanyIdOrderByDateDesc(@Param("type") ActivityTypeWarehouse type,
                                                               @Param("companyId") Long companyId);

    // ——— Hesabat (Report) üçün query-lər ———

    /** Məhsula görə gələn miqdarı hesabla */
    @Query("SELECT COALESCE(SUM(wa.quantity), 0) FROM WarehouseActivity wa " +
           "WHERE wa.product.product.id = :productId AND wa.type = 'INCOMING' " +
           "AND wa.company.id = :companyId " +
           "AND wa.date BETWEEN :startDate AND :endDate")
    Integer sumIncomingByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("companyId") Long companyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /** Məhsula görə gedən miqdarı hesabla */
    @Query("SELECT COALESCE(SUM(wa.quantity), 0) FROM WarehouseActivity wa " +
           "WHERE wa.product.product.id = :productId AND wa.type = 'OUTGOING' " +
           "AND wa.company.id = :companyId " +
           "AND wa.date BETWEEN :startDate AND :endDate")
    Integer sumOutgoingByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("companyId") Long companyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /** Məhsula görə transfer miqdarı hesabla */
    @Query("SELECT COALESCE(SUM(wa.quantity), 0) FROM WarehouseActivity wa " +
           "WHERE wa.product.product.id = :productId AND wa.type = 'TRANSFER' " +
           "AND wa.company.id = :companyId " +
           "AND wa.date BETWEEN :startDate AND :endDate")
    Integer sumTransferByProductAndDateRange(
            @Param("productId") Long productId,
            @Param("companyId") Long companyId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /** Şirkətin bütün anbar hərəkətlərini gətir */
    List<WarehouseActivity> findAllByCompanyIdOrderByDateDesc(Long companyId);

    List<WarehouseActivity> findAllByCompanyIdAndDateBetweenOrderByDateDesc(
            Long companyId,
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate);
}
