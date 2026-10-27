package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.models.Company;
import az.techvibeds.specialservice.models.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Şirkətə görə bütün faturaları tapır (pageable) */
    Page<Invoice> findAllByCompany(Company company, Pageable pageable);

    /** Şirkət + faktura növünə görə (SALES / PURCHASE) */
    Page<Invoice> findAllByCompanyAndInvoiceType(Company company, InvoiceType invoiceType, Pageable pageable);

    /** Şirkət + statusa görə filtr */
    Page<Invoice> findAllByCompanyAndStatus(Company company, InvoiceStatus status, Pageable pageable);

    /** Şirkət + növ + statusa görə filtr */
    Page<Invoice> findAllByCompanyAndInvoiceTypeAndStatus(
            Company company, InvoiceType invoiceType, InvoiceStatus status, Pageable pageable);

    /** Faktura nömrəsinə görə tap */
    Optional<Invoice> findByInvoiceNumberAndCompany(String invoiceNumber, Company company);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);

    /** Şirkətə aid bütün fakturaları siyahı kimi gətir (Excel export üçün) */
    List<Invoice> findAllByCompany(Company company);

    /** Şirkət + tarix aralığı ilə filtr */
    @Query("SELECT i FROM Invoice i WHERE i.company = :company " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findByCompanyAndDateRange(
            @Param("company") Company company,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Partner-ə aid faturaların sayı */
    long countByPartnerId(Long partnerId);

    // ——— Hesabat (Report) üçün query-lər ———

    /** Şirkətin satış fakturalarını tarix aralığı ilə gətir */
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId " +
           "AND i.invoiceType = 'SALES' " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findSalesInvoicesByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Şirkətin alış fakturalarını tarix aralığı ilə gətir */
    @Query("SELECT i FROM Invoice i WHERE i.company.id = :companyId " +
           "AND i.invoiceType = 'PURCHASE' " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findPurchaseInvoicesByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Satış toplam məbləği */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.id = :companyId AND i.invoiceType = 'SALES' " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumSalesTotalByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Alış toplam məbləği */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.company.id = :companyId AND i.invoiceType = 'PURCHASE' " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate")
    BigDecimal sumPurchaseTotalByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Detallı hesabat üçün filtrasiyalı qaimələri tapır */
    @Query("SELECT DISTINCT i FROM Invoice i JOIN FETCH i.items item WHERE i.company.id = :companyId " +
           "AND i.invoiceType = :type " +
           "AND i.invoiceDate BETWEEN :startDate AND :endDate " +
           "AND (:minAmount IS NULL OR i.totalAmount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR i.totalAmount <= :maxAmount) " +
           "ORDER BY i.invoiceDate DESC")
    List<Invoice> findInvoicesByFilters(
            @Param("companyId") Long companyId,
            @Param("type") InvoiceType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    /** Şirkət və növ üzrə ödənilməmiş (payable) fakturaları gətirir */
    @Query("SELECT i FROM Invoice i WHERE i.company = :company " +
           "AND i.invoiceType = :invoiceType " +
           "AND i.remainingAmount > 0 " +
           "AND i.status NOT IN (az.techvibeds.specialservice.enums.InvoiceStatus.DRAFT, az.techvibeds.specialservice.enums.InvoiceStatus.CANCELLED) " +
           "ORDER BY i.createdAt DESC")
    Page<Invoice> findPayableInvoices(
            @Param("company") Company company,
            @Param("invoiceType") InvoiceType invoiceType,
            Pageable pageable);
}
