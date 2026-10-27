package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TaxPaymentRequestDto {
    /** Hansı kassadan/bank hesabından ödənilir */
    private Long accountId;

    /** Vergiyə cəlb olunan məbləğ (boş olarsa avtomatik hesablanır) */
    private BigDecimal taxableAmount;

    /** Vergi faizi (default: 18%) */
    private BigDecimal taxRate;

    /** Vergi dövrü */
    private TaxPeriodType periodType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate referenceDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
