package az.techvibeds.specialservice.dtos.invoice;

import az.techvibeds.specialservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InvoiceCancelDto {
    @NotNull(message = "Kassa/Hesab seçilməlidir")
    private Long accountId;

    @NotNull(message = "Ödəniş metodu seçilməlidir")
    private PaymentMethod paymentMethod;

    private String description;
    private LocalDateTime transactionDate;
}
