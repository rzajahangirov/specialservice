package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.TransactionType;
import az.techvibeds.specialservice.models.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findByCompanyId(Long companyId);
    List<FinancialTransaction> findByCompanyIdOrderByTransactionDateDesc(Long companyId);
    Page<FinancialTransaction> findByCompanyIdOrderByTransactionDateDesc(Long companyId, Pageable pageable);
    
    List<FinancialTransaction> findByInvoiceIdOrderByTransactionDateDesc(Long invoiceId);
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinancialTransaction t WHERE t.company.id = :companyId AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCompanyAndTypeAndDateBetween(@Param("companyId") Long companyId, @Param("type") TransactionType type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ——— Hesabat (Report) üçün query-lər ———

    /** Kassaya mədaxil tranzaksiyaları (INCOME) — tarix aralığı ilə */
    @Query("SELECT t FROM FinancialTransaction t WHERE t.company.id = :companyId " +
           "AND t.type = 'INCOME' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<FinancialTransaction> findIncomeTransactionsByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /** Kassadan məxaric tranzaksiyaları (EXPENSE) — tarix aralığı ilə */
    @Query("SELECT t FROM FinancialTransaction t WHERE t.company.id = :companyId " +
           "AND t.type = 'EXPENSE' " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<FinancialTransaction> findExpenseTransactionsByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
