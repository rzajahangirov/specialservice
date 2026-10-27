package az.techvibeds.specialservice.dtos.foreignCurrency;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ForeignCurrencyPurchaseReadDto {
    private Long id;
    private String supplierName;
    private BigDecimal amount;
    private CurrencyDto currency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private BigDecimal exchangeRateDifference;
    private LocalDate purchaseDate;
    private String expenseCategory;
    private String notes;
    private LocalDateTime createdAt;
}
