package az.techvibeds.specialservice.dtos.subscription;

import az.techvibeds.specialservice.enums.BillingCycle;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionCreateDto {
    private Long companyId; 
    private Long planId;   
    private BillingCycle billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal pricePaid;
}
