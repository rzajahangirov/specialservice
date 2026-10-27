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
 * Yeni faktura yaratmaq üçün DTO.
 * UI-dakı "Faktura Detalları" formasına uyğundur.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceCreateDto {

    /** Faktura növü: SALES (satış) və ya PURCHASE (alış) */
    @NotNull(message = "Faktura növü mütləqdir (SALES və ya PURCHASE)")
    private InvoiceType invoiceType;

    /** Faktura tarixi */
    @NotNull(message = "Faktura tarixi mütləqdir")
    private LocalDate invoiceDate;

    /** Çatdırılma tarixi */
    private LocalDate deliveryDate;

    /** Müştəri və ya Təchizatçı ID-si */
    @NotNull(message = "Partner (müştəri/təchizatçı) seçilməlidir")
    private Long partnerId;

    /** Ödəniş metodu */
    private PaymentMethod paymentMethod;

    /** Valyuta */
    @NotBlank(message = "Valyuta mütləqdir")
    private String currency;

    /** Qeyd */
    @Size(max = 500, message = "Qeyd 500 simvoldan çox ola bilməz")
    private String notes;

    private BigDecimal additionalExpense;
    private AdditionalExpenseType additionalExpenseType;

    private BigDecimal discount;
    private AdditionalExpenseType discountType;

    /** Faktura sətirləri (məhsul/xidmət) */
    @Valid
    @NotEmpty(message = "Ən azı bir faktura sətri olmalıdır")
    private List<InvoiceItemDto> items;
}
