package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountReadDto {
    private Long id;
    private String name;
    private String accountNumber;
    private BigDecimal balance;
}
