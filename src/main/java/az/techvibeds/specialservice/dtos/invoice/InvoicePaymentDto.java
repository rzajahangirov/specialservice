package az.techvibeds.specialservice.dtos.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InvoicePaymentDto {
    @NotNull(message = "Kassa/Hesab seçilməlidir")
    private Long accountId;      // Hansı kassadan/hesabdan ödənilir (və ya hansına daxil olur)

    @DecimalMin(value = "0.01", message = "Ödəniş məbləği 0-dan böyük olmalıdır")
    private BigDecimal amount;   // Ödənilən məbləğ

    private Boolean fullPayment = false; // true olduqda qalıq borc qədər tam ödəniş edilir

    private String description;  // Ödəniş üçün şərh
    private LocalDateTime transactionDate; // Ödəniş tarixi
}
