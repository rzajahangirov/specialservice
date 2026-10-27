package az.techvibeds.specialservice.dtos.balace;

import az.techvibeds.specialservice.models.Partner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBalanceDto {
    private String currencyType;
    private BigDecimal amount;
    private Partner partner;
}
