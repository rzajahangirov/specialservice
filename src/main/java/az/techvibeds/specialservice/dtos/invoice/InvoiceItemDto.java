package az.techvibeds.specialservice.dtos.invoice;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Faktura sətri (item) üçün DTO.
 * UI-dakı "Məhsul İdarəetməsi" cədvəlinin hər bir sətirinə uyğundur:
 * Ad, Vahid, Qiymət (AZN), Miqdar, Barkod, Toplam.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceItemDto {

    /** Mövcud sətiri yeniləmək üçün — yeni sətir yaradılarkən null olur */
    private Long id;

    /** Məhsul/Xidmət adı */
    private String name;

    /** Ölçü vahidi (ədəd, qutu, saat, ...) */
    private String unit;

    /** Vahid qiyməti */
    @NotNull(message = "Qiymət mütləqdir")
    @DecimalMin(value = "0.01", message = "Qiymət 0-dan böyük olmalıdır")
    private BigDecimal unitPrice;

    /** Miqdar */
    @NotNull(message = "Miqdar mütləqdir")
    @Min(value = 1, message = "Miqdar ən azı 1 olmalıdır")
    private Integer quantity;

    /** Barkod */
    private String barcode;

    /** Əgər mövcud product bazadan seçilibsə — opsional */
    private Long productId;

    private Long warehouseId;

    private String vatStatus;
    private BigDecimal vatRate;

    /** Sətir üzrə endirim (faiz və ya sabit məbləğ) */
    private BigDecimal discount;
    /** Endirim növü: AMOUNT (sabit məbləğ) və ya PERCENTAGE (faiz) */
    private String discountType;

    /**
     * Maya dəyəri / Alış qiyməti.
     * Alış qaiməsində (PURCHASE) bu sahə doldurulmalıdır.
     * InvoiceServiceImpl bunu product.purchasePrice-a yazır.
     */
    private BigDecimal primeCost;
}
