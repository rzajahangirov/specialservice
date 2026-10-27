package az.techvibeds.specialservice.dtos.currency;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CurrencyRateHistoryDto {
    private BigDecimal rate;
    private LocalDateTime rateDate;
}
