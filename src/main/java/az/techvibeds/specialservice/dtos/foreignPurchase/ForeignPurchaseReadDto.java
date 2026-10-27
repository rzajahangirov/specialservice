package az.techvibeds.specialservice.dtos.foreignPurchase;

import az.techvibeds.specialservice.dtos.currency.CurrencyDto;
import az.techvibeds.specialservice.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ForeignPurchaseReadDto {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate deliveryDate;
    private String customerName;
    private String supplierName;
    private PaymentMethod paymentMethod;
    private CurrencyDto currency;
    private BigDecimal exchangeRate;
    private BigDecimal totalAmount;
    private BigDecimal totalAmountAzn;
    private List<ForeignPurchaseItemDto> items;
    private String notes;
    private LocalDateTime createdAt;
}
