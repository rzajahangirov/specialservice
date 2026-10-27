package az.techvibeds.specialservice.dtos.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringExpenseCreateDto {
    @NotBlank(message = "Şablon adı boş ola bilməz")
    private String name;

    @NotNull(message = "Məbləğ boş ola bilməz")
    @DecimalMin(value = "0.01", message = "Məbləğ 0-dan böyük olmalıdır")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Təkrarlanma tezliyi seçilməlidir (DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY)")
    private String frequency;

    @NotNull(message = "Başlanğıc tarixi boş ola bilməz")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Hesab seçilməlidir")
    private Long accountId;

    @NotNull(message = "Kateqoriya seçilməlidir")
    private Long categoryId;
}
