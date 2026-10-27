package az.techvibeds.specialservice.dtos.installment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusDto {
    private String planName;
    private LocalDate endDate;
    private LocalDate nextPaymentDate;
    private String status;
}
