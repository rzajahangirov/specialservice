package az.techvibeds.specialservice.dtos.subscriptionPlan;

import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanCreateDto {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer maxUsers;
    private Set<Long> moduleIds; 
}
