package az.techvibeds.specialservice.dtos.foreignPurchase;

import az.techvibeds.specialservice.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ForeignPurchaseCreateDto {
    private LocalDate invoiceDate;
    private LocalDate deliveryDate;
    private Long customerId;
    private Long supplierId;
    private PaymentMethod paymentMethod;
    private String currencyCode;
    private BigDecimal exchangeRate;
    private String notes;
    private List<ForeignPurchaseItemDto> items;
}
