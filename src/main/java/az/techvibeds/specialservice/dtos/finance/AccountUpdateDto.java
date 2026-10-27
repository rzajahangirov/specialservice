package az.techvibeds.specialservice.dtos.finance;

import lombok.Data;

@Data
public class AccountUpdateDto {
    private String name;
    private String accountNumber;
    private java.math.BigDecimal initialBalance;
}
