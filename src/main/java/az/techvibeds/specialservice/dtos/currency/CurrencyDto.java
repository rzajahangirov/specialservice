package az.techvibeds.specialservice.dtos.currency;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CurrencyDto {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private BigDecimal currentRate;
}
