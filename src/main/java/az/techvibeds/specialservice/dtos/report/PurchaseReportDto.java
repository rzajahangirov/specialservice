package az.techvibeds.specialservice.dtos.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alış hesabatı — faktura əsaslı alış məlumatları.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseReportDto {
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String partnerName;       // Təchizatçı adı
    private String status;
    private String paymentMethod;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;     // Ödənilmiş məbləğ
    private BigDecimal remainingAmount; // Son qalıq (qalıq borc)
    private Integer itemCount;
}
