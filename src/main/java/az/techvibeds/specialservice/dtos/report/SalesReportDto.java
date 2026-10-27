package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Satış hesabatı — faktura əsaslı satış məlumatları.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesReportDto {
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String partnerName;       // Müştəri adı
    private String status;            // Faktura statusu
    private String paymentMethod;
    private String currency;
    private BigDecimal totalAmount;    // Faktura toplam məbləği
    private BigDecimal paidAmount;     // Ödənilmiş məbləğ
    private BigDecimal remainingAmount; // Son qalıq (qalıq borc)
    private Integer itemCount;        // Fakturadakı sətir sayı
}
