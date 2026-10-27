package az.techvibeds.specialservice.repositories;

import az.techvibeds.specialservice.models.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /** Fakturaya aid bütün sətirləri gətir */
    List<InvoiceItem> findAllByInvoiceId(Long invoiceId);

    /** Fakturaya aid bütün sətirləri sil */
    void deleteAllByInvoiceId(Long invoiceId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT new az.techvibeds.specialservice.dtos.dashboard.TopSellingCategoryDto(c.name, SUM(CAST(i.quantity AS long))) " +
        "FROM InvoiceItem i " +
        "JOIN i.invoice inv " +
        "JOIN i.product p " +
        "JOIN p.category c " +
        "WHERE inv.company.id = :companyId " +
        "AND inv.invoiceType = 'SALES' " +
        "AND inv.isDeleted = false " +
        "GROUP BY c.name " +
        "ORDER BY SUM(CAST(i.quantity AS long)) DESC"
    )
    List<az.techvibeds.specialservice.dtos.dashboard.TopSellingCategoryDto> findTopSellingCategories(@org.springframework.data.repository.query.Param("companyId") Long companyId);
}
