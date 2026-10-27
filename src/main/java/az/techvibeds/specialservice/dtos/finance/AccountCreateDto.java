package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountCreateDto {
    private String name;
    private String accountNumber;
    private BigDecimal initialBalance;
}
