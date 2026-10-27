package az.techvibeds.specialservice.dtos.invoice;

import az.techvibeds.specialservice.enums.AdditionalExpenseType;
import az.techvibeds.specialservice.enums.InvoiceStatus;
import az.techvibeds.specialservice.enums.InvoiceType;
import az.techvibeds.specialservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Faktura oxumaq üçün cavab DTO-su.
 * Siyahıda və detail səhifəsində istifadə olunur.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceReadDto {

    private Long id;

    /** Faktura nömrəsi (INV-2024-001) */
    private String invoiceNumber;

    /** Faktura növü */
    private InvoiceType invoiceType;

    /** Faktura statusu */
    private InvoiceStatus status;

    /** Faktura tarixi */
    private LocalDate invoiceDate;

    /** Çatdırılma tarixi */
    private LocalDate deliveryDate;

    /** Ödəniş metodu */
    private PaymentMethod paymentMethod;

    /** Valyuta */
    private String currency;

    /** Ümumi məbləğ */
    private BigDecimal totalAmount;

    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private BigDecimal additionalExpense;
    private AdditionalExpenseType additionalExpenseType;
    private BigDecimal discount;
    private AdditionalExpenseType discountType;
    private BigDecimal totalVatAmount;
    private BigDecimal grossAmount;

    private Boolean stockApplied;

    /** Qeyd */
    private String notes;

    /** Yaradılma tarixi */
    private LocalDateTime createdAt;

    // ——— Partner məlumatları ———

    private Long partnerId;
    private String partnerName;
    private String partnerType;

    // ——— Faktura sətirləri ———

    private List<InvoiceItemReadDto> items;

    // ——— Ödəniş tarixçəsi ———

    private List<InvoicePaymentReadDto> payments;
}
