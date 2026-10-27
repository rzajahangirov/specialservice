package az.techvibeds.specialservice.dtos.invoice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Faktura sətri oxumaq üçün cavab DTO-su.
 * UI-dakı "Məhsul İdarəetməsi" cədvəlinin hər sətrini təmsil edir.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceItemReadDto {

    private Long id;

    /** Məhsul/Xidmət adı */
    private String name;

    /** Ölçü vahidi */
    private String unit;

    /** Vahid qiyməti */
    private BigDecimal unitPrice;

    /** Miqdar */
    private Integer quantity;

    /** Barkod */
    private String barcode;

    /** Sətir toplam məbləği */
    private BigDecimal lineTotal;

    /** Əlaqəli product ID (əgər varsa) */
    private Long productId;

    private Long warehouseId;
    private String warehouseName;

    /** Sətir üzrə ƏDV məbləği */
    private BigDecimal vatAmount;

    private String vatStatus;
    private BigDecimal vatRate;
    private BigDecimal primeCost;

    /** Sətir üzrə endirim dəyəri */
    private BigDecimal discount;
    /** Endirim növü: AMOUNT və ya PERCENTAGE */
    private String discountType;
    /** Hesablanmış endirim məbləği */
    private BigDecimal discountAmount;
}
