package az.techvibeds.specialservice.dtos.subscription;

import az.techvibeds.specialservice.enums.BillingCycle;
import lombok.Data;

@Data
public class SubscribeRequestDto {
    private Long planId;
    private BillingCycle billingCycle; // MONTHLY və ya YEARLY (default: YEARLY)
}

