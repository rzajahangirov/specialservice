package az.techvibeds.specialservice.dtos.invoice;

import az.techvibeds.specialservice.enums.AdditionalExpenseType;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mövcud fakturanı yeniləmək üçün DTO.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceUpdateDto {

    /** Yenilənəcək fakturanın ID-si */
    @NotNull(message = "Faktura ID-si mütləqdir")
    private Long id;

    /** Faktura növü */
    private InvoiceType invoiceType;

    /** Faktura tarixi */
    private LocalDate invoiceDate;

    /** Çatdırılma tarixi */
    private LocalDate deliveryDate;

    /** Partner ID-si */
    private Long partnerId;

    /** Ödəniş metodu */
    private PaymentMethod paymentMethod;

    /** Valyuta */
    private String currency;

    /** Qeyd */
    @Size(max = 500, message = "Qeyd 500 simvoldan çox ola bilməz")
    private String notes;

    private BigDecimal additionalExpense;
    private AdditionalExpenseType additionalExpenseType;

    private BigDecimal discount;
    private AdditionalExpenseType discountType;

    /** Yenilənmiş sətir siyahısı (tamamilə əvəz edir) */
    @Valid
    private List<InvoiceItemDto> items;
}
