package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.ExpenseCategory;
import az.techvibeds.specialservice.models.IndirectExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface IndirectExpenseRepository extends JpaRepository<IndirectExpense, Long> {
    @Query("SELECT ie FROM IndirectExpense ie " +
            "WHERE ie.company.id = :companyId " +
            "AND (:category IS NULL OR ie.category = :category)")
    List<IndirectExpense> findByCompanyAndCategory(
            @Param("companyId") Long companyId,
            @Param("category") ExpenseCategory category);

    @Query("SELECT ie FROM IndirectExpense ie " +
            "WHERE ie.company.id = :companyId " +
            "AND (:category IS NULL OR ie.category = :category) " +
            "AND (LOWER(ie.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR CAST(ie.id AS string) LIKE CONCAT('%', :keyword, '%'))")
    List<IndirectExpense> findByCompanyAndCategoryAndKeyword(
            @Param("companyId") Long companyId,
            @Param("category") ExpenseCategory category,
            @Param("keyword") String keyword);

    @Query("SELECT SUM(ie.amount) FROM IndirectExpense ie")
    java.math.BigDecimal sumAllAmounts();

    @Query("SELECT COALESCE(SUM(ie.amount), 0) FROM IndirectExpense ie " +
            "WHERE ie.company.id = :companyId " +
            "AND ie.expenseDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumByCompanyAndDateRange(@Param("companyId") Long companyId,
                                                  @Param("startDate") Date startDate,
                                                  @Param("endDate") Date endDate);

    /** Hesabat üçün: şirkətin xərclərini tarix aralığı ilə gətir */
    @Query("SELECT ie FROM IndirectExpense ie WHERE ie.company.id = :companyId " +
           "AND ie.expenseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ie.expenseDate DESC")
    List<IndirectExpense> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);
}
