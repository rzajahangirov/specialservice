package az.techvibeds.specialservice.dtos.subscription;

import az.techvibeds.specialservice.dtos.company.CompanyReadDto;
import az.techvibeds.specialservice.dtos.subscriptionPlan.SubscriptionPlanReadDto;
import az.techvibeds.specialservice.enums.BillingCycle;
import az.techvibeds.specialservice.enums.SubscriptionStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionReadDto {
    private Long id;
    private CompanyReadDto company;
    private SubscriptionPlanReadDto plan;
    private SubscriptionStatus status;
    private BillingCycle billingCycle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal pricePaid;
}
