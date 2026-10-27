package az.techvibeds.specialservice.dtos.invoice;

import az.techvibeds.specialservice.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InvoicePartialReturnDto {
    @NotNull(message = "Məhsul sətri seçilməlidir")
    private Long invoiceItemId;

    @NotNull(message = "Qaytarılan miqdar qeyd olunmalıdır")
    @Min(value = 1, message = "Qaytarılan miqdar ən azı 1 olmalıdır")
    private Integer quantity;

    @NotNull(message = "Kassa/Hesab seçilməlidir")
    private Long accountId;

    @NotNull(message = "Ödəniş metodu seçilməlidir")
    private PaymentMethod paymentMethod;

    private String description;
    private LocalDateTime transactionDate;
}
