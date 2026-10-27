package az.techvibeds.specialservice.dtos.foreignCurrency;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ForeignCurrencyPurchaseCreateDto {
    private String supplierName;
    private BigDecimal amount;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private LocalDate purchaseDate;
    private String expenseCategory;
    private String notes;
}
