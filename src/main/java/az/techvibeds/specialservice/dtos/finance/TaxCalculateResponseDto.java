package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TaxCalculateResponseDto {
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private TaxPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDateTime generatedAt;

    /** Ödəniş edilib-edilmədiyini göstərir */
    private Boolean paid = false;
    /** Ödəniş edildikdə — hansı kassadan */
    private Long accountId;
    private String accountName;
    /** Yaradılmış maliyyə tranzaksiyasının ID-si */
    private Long transactionId;
}
