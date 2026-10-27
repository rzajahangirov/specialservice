package az.techvibeds.specialservice.dtos.payroll;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollPaymentDto {
    @NotNull(message = "Account ID is required when paying salary")
    private Long accountId;

    private boolean isPaid;
}
