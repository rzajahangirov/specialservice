package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TaxCalculateRequestDto {
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private TaxPeriodType periodType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate referenceDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
