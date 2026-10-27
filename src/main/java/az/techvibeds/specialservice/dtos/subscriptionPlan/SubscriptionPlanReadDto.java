package az.techvibeds.specialservice.dtos.subscriptionPlan;

import az.techvibeds.specialservice.dtos.module.ModuleReadDto;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanReadDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer maxUsers;
    private Set<ModuleReadDto> modules; 
}
